package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;

public interface ApArticleConfigService extends IService<ApArticleConfig> {
    /*修改文章配置是否下架
     */
    void downArticle(WmNewsDto newsDto);

}
