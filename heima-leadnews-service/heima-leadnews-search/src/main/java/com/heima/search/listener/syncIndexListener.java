package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.heima.model.search.vos.SearchArticleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.heima.common.Constant.ArticleConstant.ARTICLE_ES_SYNC_TOPIC;

@Component
@Slf4j
public class syncIndexListener {
    @KafkaListener(topics = {ARTICLE_ES_SYNC_TOPIC})
    public void syncIndex(String text){
        if (StringUtils.isBlank(text))throw new RuntimeException("当前消息为空，请检查消息中间件");


    }
}
