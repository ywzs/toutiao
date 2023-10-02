package com.heima.article.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.heima.common.Constant.ArticleConstant.*;

@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {
    @Resource
    ApArticleMapper apArticleMapper;
    @Resource
    IWemediaClient wemediaClient;
    @Resource
    CacheService cacheService;

    @Override
    public void computerHotArticle() {
        //1.查询出前5天的热点文章
        Date date = DateTime.now().minusDays(5).toDate();
        List<ApArticle> articleList = apArticleMapper.findArticle5days(date);
        //2.计算分值
        List<HotArticleVo> hotArticleVoList = computerScore(articleList);
        //3.为每个频道缓存数据
        cacheHotToRedis(hotArticleVoList);
    }

    private void cacheHotToRedis(List<HotArticleVo> hotArticleVoList) {
        ResponseResult result = wemediaClient.findAll();
        if (result.getCode().equals(200)){
            String s = JSON.toJSONString(result.getData());
            List<WmChannel> wmChannels = JSON.parseArray(s, WmChannel.class);
            if (wmChannels!=null && !wmChannels.isEmpty()){
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> articleVos = hotArticleVoList.stream()
                            .filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    sortAndCache(articleVos, HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }
        //默认tag  就是整体排序
        sortAndCache(hotArticleVoList, HOT_ARTICLE_FIRST_PAGE+DEFAULT_TAG);
    }

    private void sortAndCache(List<HotArticleVo> articleVos, String key) {
        articleVos = articleVos.stream()
                .sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        if (articleVos.size() > 30) {
            articleVos = articleVos.subList(0, 30);  //只需要30条
        }
        cacheService.set(key, JSON.toJSONString(articleVos));
    }

    private List<HotArticleVo> computerScore(List<ApArticle> articleList) {
        List<HotArticleVo> voArrayList = new ArrayList<>();
        if (articleList != null && !articleList.isEmpty()) {
            for (ApArticle article : articleList) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(article, hot);
                Integer score = getScore(article);
                hot.setScore(score);
                voArrayList.add(hot);
            }
        }
        return voArrayList;
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
