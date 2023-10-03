package com.heima.article.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.heima.common.Constant.ArticleConstant;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.heima.common.Constant.ArticleConstant.*;
import static com.heima.common.Constant.ArticleConstant.HOT_ARTICLE_COLLECTION_WEIGHT;

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
    @Resource
    CacheService cacheService;

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
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstant.DEFAULT_TAG);
        }
        //判断时间
        if (dto.getMaxBehotTime() == null) dto.setMaxBehotTime(new Date());
        if (dto.getMinBehotTime() == null) dto.setMinBehotTime(new Date());
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);
        return ResponseResult.okResult(apArticles);
    }

    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        //1.检查参数
        if (dto == null) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(dto, article);
        //2.判断是否存在id
        if (dto.getId() == null) {
            //2.1不存在就是保存文章  文章配置  文章内容
            save(article);
            ApArticleConfig config = new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(config);
            ApArticleContent content = new ApArticleContent();
            content.setArticleId(article.getId());
            content.setContent(dto.getContent());
            apArticleContentMapper.insert(content);
        } else {
            //2.2存在就是修改文章   文章内容
            updateById(article);
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne
                    (Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, article.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }
        //生成静态模板文件上传minio
        articleFreemarkerService.buildArticleToMinio(article, dto.getContent());

        return ResponseResult.okResult(article.getId());
    }

    @Override
    public void updateScore(ArticleVisitStreamMess mess) {
        ApArticle article = updateArticle(mess);
        //1.更新文章的阅读、点赞、收藏、评论的数量
        ApArticle apArticle = updateArticle(mess);
        //2.计算文章的分值
        Integer score = getScore(apArticle);
        score = score * 3;

        //3.替换当前文章对应频道的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstant.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

        //4.替换推荐对应的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstant.HOT_ARTICLE_FIRST_PAGE + ArticleConstant.DEFAULT_TAG);
    }

    /**
     * 替换数据并且存入到redis
     *
     * @param apArticle
     * @param score
     * @param s
     */
    private void replaceDataToRedis(ApArticle apArticle, Integer score, String s) {
        String articleListStr = cacheService.get(s);
        if (StringUtils.isNotBlank(articleListStr)) {
            List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);

            boolean flag = true;

            //如果缓存中存在该文章，只更新分值
            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }

            //如果缓存中不存在，查询缓存中分值最小的一条数据，进行分值的比较，如果当前文章的分值大于缓存中的数据，就替换
            if (flag) {
                if (hotArticleVoList.size() >= 30) {
                    hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                    HotArticleVo lastHot = hotArticleVoList.get(hotArticleVoList.size() - 1);
                    if (lastHot.getScore() < score) {
                        hotArticleVoList.remove(lastHot);
                        HotArticleVo hot = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hot);
                        hot.setScore(score);
                        hotArticleVoList.add(hot);
                    }


                } else {
                    HotArticleVo hot = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hot);
                    hot.setScore(score);
                    hotArticleVoList.add(hot);
                }
            }
            //缓存到redis
            hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            cacheService.set(s, JSON.toJSONString(hotArticleVoList));

        }
    }

    private ApArticle updateArticle(ArticleVisitStreamMess mess) {
        ApArticle apArticle = getById(mess.getArticleId());
        apArticle.setCollection(apArticle.getCollection() == null ? 0 : apArticle.getCollection() + mess.getCollect());
        apArticle.setComment(apArticle.getComment() == null ? 0 : apArticle.getComment() + mess.getComment());
        apArticle.setLikes(apArticle.getLikes() == null ? 0 : apArticle.getLikes() + mess.getLike());
        apArticle.setViews(apArticle.getViews() == null ? 0 : apArticle.getViews() + mess.getView());
        updateById(apArticle);
        return apArticle;
    }

    private Integer getScore(ApArticle article) {
        int score = 0;
        if (article.getLikes() != null) score += article.getLikes() * HOT_ARTICLE_LIKE_WEIGHT;
        if (article.getViews() != null) score += article.getViews() * HOT_ARTICLE_VIEW_WEIGHT;
        if (article.getComment() != null) score += article.getComment() * HOT_ARTICLE_COMMENT_WEIGHT;
        if (article.getCollection() != null) score += article.getCollection() * HOT_ARTICLE_COLLECTION_WEIGHT;
        return score;
    }
}
