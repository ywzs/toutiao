package com.heima.schedule.fegin;

import com.heima.apis.schedule.IScheduleClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
public class ScheduleClient implements IScheduleClient {
    @Resource
    TaskService taskService;

    @PostMapping("/api/v1/task/add")
    public ResponseResult addTask(@RequestBody Task task) {
        return ResponseResult.okResult(taskService.addTask(task));
    }

    @GetMapping("/api/v1/task/{taskId}")
    public ResponseResult cancelTask(@PathVariable long taskId) {
        return ResponseResult.okResult(taskService.cancelTask(taskId));
    }

    @GetMapping("/api/v1/task/{type}/{priority}")
    public ResponseResult poll(@PathVariable int type, @PathVariable int priority) {
        return ResponseResult.okResult(taskService.poll(type,priority));
    }
}
