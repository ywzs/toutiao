package com.heima.schedule.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.Constant.RedisKeyConstants;
import com.heima.common.Constant.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Resource
    TaskinfoMapper taskinfoMapper;
    @Resource
    TaskinfoLogsMapper taskinfoLogsMapper;
    @Resource
    CacheService cacheService;

    @Override
    public long addTask(Task task) {
        if (task == null) throw new RuntimeException("任务为空");
        //1.添加任务到数据库
        boolean isOk = addTaskToDB(task);
        if (!isOk) throw new RuntimeException("任务保存失败");
        //2.添加任务到redis
        addTaskToCache(task);
        return task.getTaskId();
    }


    /**
     * 添加任务到redis
     *
     * @param task 任务
     */
    private void addTaskToCache(Task task) {
        //3.判断是向list（立即执行）还是zset（未来的一段时间后执行）添加
//        long prevTime = System.currentTimeMillis() + ScheduleConstants.ENTER_CACHE_TIME*60*1000;
        Calendar calendar = Calendar.getInstance(); //预设时间5分钟
        calendar.add(Calendar.MINUTE, ScheduleConstants.ENTER_CACHE_TIME);
        long prevTime = calendar.getTimeInMillis();
        String key = task.getTaskType() + ":" + task.getPriority();
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            //存入list
            cacheService.lLeftPush(RedisKeyConstants.TASK_NOW_PREV + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= prevTime) {
            cacheService.zAdd(RedisKeyConstants.TASK_FUTURE_PREV + key, JSON.toJSONString(task), task.getExecuteTime());
        }
    }

    /**
     * 添加任务到数据库
     *
     * @param task 任务
     */
    private boolean addTaskToDB(Task task) {
        boolean flag = true;
        try {
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);
            //设置任务id
            task.setTaskId(taskinfo.getTaskId());
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(0);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    @Override
    public boolean cancelTask(long taskId) {
        boolean flag = false;
        //删除任务，跟新任务日志
        Task task = updateDB(taskId, ScheduleConstants.CANCELLED);
        if (task != null) {
            removeTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    /**
     * 删除redis数据
     *
     * @param task 任务
     */
    private void removeTaskFromCache(Task task) {
        String key = task.getTaskType() + ":" + task.getPriority();
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lRemove(RedisKeyConstants.TASK_NOW_PREV + key, 0, JSON.toJSONString(task));
        } else {
            cacheService.zRemove(RedisKeyConstants.TASK_FUTURE_PREV + key, JSON.toJSONString(task));
        }
    }

    private Task updateDB(long taskId, int status) {
        Task task = null;
        try {
            taskinfoMapper.deleteById(taskId);
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (Exception e) {
            log.error("task cancel fail with id = {}", taskId);
        }
        return task;
    }

    @Override
    public Task poll(int type, int priority) {
        String key = type + ":" + priority;
        String task_json = cacheService.lRightPop(RedisKeyConstants.TASK_NOW_PREV + key);
        Task task = null;
        if (StringUtils.isNotBlank(task_json)) {
            task = JSON.parseObject(task_json, Task.class);
            updateDB(task.getTaskId(), ScheduleConstants.EXECUTED);
        }
        return task;
    }

    /**
     * 定时任务，每分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh() {
        //分布式锁（redis 的 setnx实现）
        String token = cacheService.tryLock(RedisKeyConstants.TASK_SYNC_KEYS, RedisKeyConstants.TASK_SYNC_TIME_MS);
        if (StringUtils.isNotBlank(token)) {
            log.info("定时任务---->刷新keys");
            Set<String> keys = cacheService.scan(RedisKeyConstants.TASK_FUTURE_PREV + "*");
            for (String key : keys) {
                //采用pipeline技术提升性能
                String now_key = key.split(RedisKeyConstants.TASK_FUTURE_PREV)[1];
                Set<String> tasks = cacheService.zRangeByScore(key, 0, System.currentTimeMillis());
                if (!tasks.isEmpty()) {
                    cacheService.refreshWithPipeline(key, now_key, tasks);
                    log.info("刷新成功{}----->{}", key, now_key);
                }
            }
        }
    }

    /**
     * 数据库任务定时同步
     */
    @PostConstruct   //微服务一启动就会执行一次
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadData() {
        String token = cacheService.tryLock(RedisKeyConstants.TASK_SYNC_DB, RedisKeyConstants.TASK_SYNC_TIME_MS);
        if (StringUtils.isNotBlank(token)) {
            //清理缓存数据
            clearCache();
            //查询数据库合适任务
            Calendar calendar = Calendar.getInstance(); //预设时间5分钟
            calendar.add(Calendar.MINUTE, ScheduleConstants.ENTER_CACHE_TIME);
            Date time = calendar.getTime();
            List<Taskinfo> taskinfos = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, time));
            //添加任务到缓存
            if (taskinfos != null && !taskinfos.isEmpty()) {
                for (Taskinfo taskinfo : taskinfos) {
                    Task task = new Task();
                    BeanUtils.copyProperties(taskinfo, task);
                    task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                    addTaskToCache(task);
                }
            }
            log.info("完成数据库向redis的任务同步");
        }
    }

    public void clearCache() {
        //清理缓存中所有的任务数据
        Set<String> futureKeys = cacheService.scan(RedisKeyConstants.TASK_FUTURE_PREV + "*");
        Set<String> nowKeys = cacheService.scan(RedisKeyConstants.TASK_NOW_PREV + "*");
        cacheService.delete(futureKeys);
        cacheService.delete(nowKeys);
    }
}
