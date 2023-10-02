package com.heima.schedule.service.Impl;


import com.heima.common.Constant.ScheduleConstants;
import com.heima.model.common.enums.TaskTypeEnum;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
class TaskServiceImplTest {
    @Resource
    TaskService taskService;
    @Test
    public void addTask1(){
        Task task = new Task();
        task.setExecuteTime(new Date().getTime());
        task.setTaskType(TaskTypeEnum.REMOTEERROR.getTaskType());
        task.setPriority(TaskTypeEnum.REMOTEERROR.getPriority());
        taskService.addTask(task);
    }
    @Test
    public void addTask2(){
        Task task = new Task();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,4);
        long timeInMillis = calendar.getTimeInMillis();
        task.setExecuteTime(timeInMillis);
        task.setTaskType(TaskTypeEnum.REMOTEERROR.getTaskType());
        task.setPriority(TaskTypeEnum.REMOTEERROR.getPriority());
        taskService.addTask(task);
    }

    @Test
    public void cancelTask(){
        System.out.println(taskService.cancelTask(1706628653760045058L));
    }

}