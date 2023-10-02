package com.heima.article.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.heima.common.Constant.ArticleConstant;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
    @Resource
    ApArticleMapper apArticleMapper;
    @Resource
    ApArticleConfigMapper apArticleConfigMapper;
    @Resource
    ApArticleContentMapper apArticleContentMapper;
    @Resource
    ArticleFreemarkerService articleFreemarkerService;

    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //参数校验
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            size = 10;
        }
        //设置最大分页数
        size = Math.min(size, ArticleConstant.MAX_SEARCH_SIZE);
        dto.setSize(size);
        //判断type是否合理
        if (type == null ||
                (!type.equals(ArticleConstant.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstant.LOADTYPE_LOAD_NEW))) {
            type = ArticleConstant.LOADTYPE_LOAD_MORE;
        }
        //判断tag
        if (StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstant.DEFAULT_TAG);
        }
        //判断时间
        if (dto.getMaxBehotTime() == null)dto.setMaxBehotTime(new Date());
        if (dto.getMinBehotTime() == null)dto.setMinBehotTime(new Date());
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);
        return ResponseResult.okResult(apArticles);
    }

    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        //1.检查参数
        if(dto==null)return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(dto,article);
        //2.判断是否存在id
        if (dto.getId()==null){
            //2.1不存在就是保存文章  文章配置  文章内容
            save(article);
            ApArticleConfig config = new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(config);
            ApArticleContent content = new ApArticleContent();
            content.setArticleId(article.getId());
            content.setContent(dto.getContent());
            apArticleContentMapper.insert(content);
        }else {
            //2.2存在就是修改文章   文章内容
            updateById(article);
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne
                    (Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, article.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }
        //生成静态模板文件上传minio
        articleFreemarkerService.buildArticleToMinio(article,dto.getContent());

        return ResponseResult.okResult(article.getId());
    }


}
