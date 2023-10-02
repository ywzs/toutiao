package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.vos.SearchArticleVo;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

public interface ArticleSearchService {
    /**
     * 条件查询
     * @param dto  数据
     * @return  结果
     * @throws IOException
     */
    public ResponseResult search(UserSearchDto dto) throws IOException;

    /**
     * 同步文章索引库(接受异步消息)
     * @param text 索引信息
     * @return
     */
    void syncIndex(String text);
}
