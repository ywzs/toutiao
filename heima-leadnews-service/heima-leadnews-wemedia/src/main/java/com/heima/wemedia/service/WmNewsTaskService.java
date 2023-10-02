package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {
    /**
     * 调用远程服务来进行延时任务控制
     * @param id  id
     * @param publishTime  发布时间
     */
    public void addNewsToTask(Integer id, Date publishTime);

    /**
     * 消费任务审核文章
     */
    public void scanNewsByTask();
}
