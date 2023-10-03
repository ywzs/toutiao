package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.common.Constant.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class hotArticleHandler {
    @Bean
    KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        //接受消息
        KStream<String, String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);
        //处理消息
        stream.map((key, value) -> {
                    UpdateArticleMess articleMess = JSON.parseObject(value, UpdateArticleMess.class);
                    //重置key 和 value   id  和 操作
                    return new KeyValue<>(articleMess.getArticleId().toString(),
                            articleMess.getType().name() + ":" + articleMess.getAdd());
                })
                .groupBy((key, value) -> key)   //更具id 聚合
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                .aggregate(new Initializer<String>() {
                    //初始聚合
                    @Override
                    public String apply() {
                        return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                    }
                }, new Aggregator<String, String, String>() {
                    //正式处理的聚合
                    @Override
                    public String apply(String key, String value, String aggVaule) {
                        if (StringUtils.isBlank(value)) return aggVaule;
                        int col = 0, com = 0, lik = 0, vie = 0;
                        //初始化操作
                        for (String agg : aggVaule.split(",")) {
                            String[] split = agg.split(":");
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])) {
                                case COLLECTION:
                                    col = Integer.parseInt(split[1]);
                                    break;
                                case COMMENT:
                                    com = Integer.parseInt(split[1]);
                                    break;
                                case LIKES:
                                    lik = Integer.parseInt(split[1]);
                                    break;
                                case VIEWS:
                                    vie = Integer.parseInt(split[1]);
                                    break;
                            }
                        }
                        //累加操作
                        String[] valAry = value.split(":");
                        switch (UpdateArticleMess.UpdateArticleType.valueOf(valAry[0])) {
                            case COLLECTION:
                                col += Integer.parseInt(valAry[1]);
                                break;
                            case COMMENT:
                                com += Integer.parseInt(valAry[1]);
                                break;
                            case LIKES:
                                lik += Integer.parseInt(valAry[1]);
                                break;
                            case VIEWS:
                                vie += Integer.parseInt(valAry[1]);
                                break;
                        }
                        String formatStr = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", col, com, lik, vie);
                        System.out.println("文章的id:"+key);
                        System.out.println("当前时间窗口内的消息处理结果："+formatStr);
                        return formatStr;
                    }
                }, Materialized.as("hot-article-stream-001")).toStream()
                .map((key,value)->{
                    return new KeyValue<>(key.key().toString(),formatObj(key.key().toString(),value));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);
        return stream;
    }

    private Object formatObj(String articleId,String value) {
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        mess.setArticleId(Long.valueOf(articleId));
        //COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0
        String[] valAry = value.split(",");
        for (String val : valAry) {
            String[] split = val.split(":");
            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                case COLLECTION:
                    mess.setCollect(Integer.parseInt(split[1]));
                    break;
                case COMMENT:
                    mess.setComment(Integer.parseInt(split[1]));
                    break;
                case LIKES:
                    mess.setLike(Integer.parseInt(split[1]));
                    break;
                case VIEWS:
                    mess.setView(Integer.parseInt(split[1]));
                    break;
            }
        }
        log.info("聚合消息处理之后的结果为:{}",JSON.toJSONString(mess));
        return JSON.toJSONString(mess);
    }
}
