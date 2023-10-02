package com.heima.wemedia.service;

/**
 * 自媒体文章自动审核
 */
public interface WmNewsAutoScan {
    /**
     * 根据文章id自动审核文章
     * @param id   文章id
     * @param test true 是测试  false 真实调用阿里云接口
     */
    public void AutoScan(Integer id,Boolean test);
}
