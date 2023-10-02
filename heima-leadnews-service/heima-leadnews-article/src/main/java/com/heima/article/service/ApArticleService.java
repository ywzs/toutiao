package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.web.bind.annotation.RequestBody;


public interface ApArticleService extends IService<ApArticle> {
    /**
     * 加载文章接口
     * @param dto
     * @param type  1.代表加载更多 2. 代表加载新的
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto,Short type);

    /**
     * 保存或者修改文章
     * @param dto 数据
     * @return id
     */
    public ResponseResult saveArticle(ArticleDto dto);
}
