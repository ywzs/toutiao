package com.heima.article.controller.v1;

import com.heima.article.service.ApArticleService;
import com.heima.common.Constant.ArticleConstant;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/article")
public class ArticleHomeController {
    @Resource
    ApArticleService apArticleService;
    /**
     * 加载首页
     */
    @PostMapping("/load")
    public ResponseResult load(@RequestBody ArticleHomeDto articleHomeDto){
        return apArticleService.load(articleHomeDto, ArticleConstant.LOADTYPE_LOAD_MORE);
    }
    /**
     * 加载更多
     */
    @PostMapping("/loadmore")
    public ResponseResult loadMore(@RequestBody ArticleHomeDto articleHomeDto){
        return apArticleService.load(articleHomeDto, ArticleConstant.LOADTYPE_LOAD_MORE);
    }
    /**
     * 加载最新
     */
    @PostMapping("/loadnew")
    public ResponseResult loadNew(@RequestBody ArticleHomeDto articleHomeDto){
        return apArticleService.load(articleHomeDto, ArticleConstant.LOADTYPE_LOAD_NEW);
    }
}
