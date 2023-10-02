package com.heima.apis.schedule;

import com.heima.apis.schedule.fallback.IScheduleClientFallback;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "leadnews-schedule",fallback = IScheduleClientFallback.class)//,fallback = IScheduleClientFallback.class
public interface IScheduleClient {
    /**
     * 添加任务
     *
     * @param task 任务
     * @return 任务id
     */
    @PostMapping("/api/v1/task/add")
    public ResponseResult addTask(@RequestBody Task task);

    /**
     * 取消任务
     *
     * @param taskId 任务id
     * @return 是否成功
     */
    @GetMapping("/api/v1/task/{taskId}")
    public ResponseResult cancelTask(@PathVariable long taskId);

    /**
     * 根据类型和任务优先级拉去任务
     *
     * @param type     类型
     * @param priority 优先级
     * @return 任务
     */
    @GetMapping("/api/v1/task/{type}/{priority}")
    public ResponseResult poll(@PathVariable int type, @PathVariable int priority);
}
