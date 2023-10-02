package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.wemedia.dtos.WmNewsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.heima.common.Constant.WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC;

@Component
@Slf4j
public class articleIsDownListener {
    @Resource
    ApArticleConfigService apArticleConfigService;
    @KafkaListener(topics = {WM_NEWS_UP_OR_DOWN_TOPIC})
    public void handleIsDown(String message){
        if (StringUtils.isNotBlank(message)){
            WmNewsDto newsDto = JSON.parseObject(message, WmNewsDto.class);
            //修改消息
            apArticleConfigService.downArticle(newsDto);
        }
    }
}
