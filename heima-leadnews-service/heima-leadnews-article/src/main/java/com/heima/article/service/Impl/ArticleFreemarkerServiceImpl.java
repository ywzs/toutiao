package com.heima.article.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.heima.common.Constant.ArticleConstant.ARTICLE_ES_SYNC_TOPIC;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {
    @Resource
    Configuration configuration;
    @Resource
    FileStorageService fileStorageService;
    @Resource
    ApArticleService apArticleService;
    @Resource
    KafkaTemplate<String,String> kafkaTemplate;
    //构建静态文件上传minio
    @Override
    @Async
    public void buildArticleToMinio(ApArticle apArticle, String content) {
        if (StringUtils.isNotBlank(content)) {
            try {
                //2.生成html
                Template template = configuration.getTemplate("article.ftl");
                //获取数据模型
                Map<String, Object> map = new HashMap<>();
                map.put("content", JSONArray.parseArray(content));
                StringWriter out = new StringWriter();
                template.process(map, out);
                //3.上传minio
                InputStream in = new ByteArrayInputStream(out.toString().getBytes(StandardCharsets.UTF_8));
                String url = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", in);
                //4.保存文件路径
                apArticleService.update(Wrappers.<ApArticle>lambdaUpdate()
                        .eq(ApArticle::getId, apArticle.getId())
                        .set(StringUtils.isNotBlank(url), ApArticle::getStaticUrl, url));
                //5.发送消息通知 搜索服务添加索引库
                createArticleIndex(apArticle,content,url);
            } catch (IOException | TemplateException e) {
                e.printStackTrace();
            }
        }
    }

    private void createArticleIndex(ApArticle apArticle, String content, String url) {
        SearchArticleVo searchArticleVo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle,searchArticleVo);
        searchArticleVo.setContent(content);
        searchArticleVo.setStaticUrl(url);
        kafkaTemplate.send(ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(searchArticleVo));
    }
}
