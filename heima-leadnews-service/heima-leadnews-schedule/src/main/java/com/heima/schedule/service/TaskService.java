package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

public interface TaskService {
    /**
     * 添加任务
     * @param task 任务
     * @return 任务id
     */
    long addTask(Task task);

    /**
     * 取消任务
     * @param taskId 任务id
     * @return 是否成功
     */
    boolean cancelTask(long taskId);

    /**
     * 根据类型和任务优先级拉去任务
     * @param type  类型
     * @param priority  优先级
     * @return 任务
     */
    Task poll(int type,int priority);
}
