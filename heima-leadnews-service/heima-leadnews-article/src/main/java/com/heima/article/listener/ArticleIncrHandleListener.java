package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApArticleService;
import com.heima.common.Constant.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class ArticleIncrHandleListener {
    @Resource
    ApArticleService apArticleService;
    @KafkaListener(topics = HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC)
    public void onMessage(String mess){
        if (StringUtils.isNotBlank(mess)){
            ArticleVisitStreamMess mess1 = JSON.parseObject(mess, ArticleVisitStreamMess.class);
            apArticleService.updateScore(mess1);
        }
    }
}
