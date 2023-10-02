package com.heima.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.pojos.ApUserSearch;

public interface ApUserSearchService {
    /**
     * 保存用户搜索历史记录（异步）
     * @param keyWord  关键词
     * @param userId  用户id
     */
    public void insert(String keyWord, Integer userId);

    /**
     * 加载用户历史记录
     */
    ResponseResult loadHistory();

    /**
     * 删除用户历史记录
     * @return
     */
    ResponseResult delHistory(HistorySearchDto dto);
}