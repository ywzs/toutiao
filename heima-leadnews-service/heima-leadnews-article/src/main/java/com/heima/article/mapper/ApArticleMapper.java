package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {
    /**
     * 加载文章
     * @param dto
     * @param type  1.加载更多    2.加载新的
     * @return
     */
    public List<ApArticle> loadArticleList(ArticleHomeDto dto, Short type);
}
