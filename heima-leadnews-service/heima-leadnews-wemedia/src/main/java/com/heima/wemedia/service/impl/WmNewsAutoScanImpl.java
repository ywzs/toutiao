package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanImpl implements WmNewsAutoScan {
    @Resource
    private WmNewsMapper wmNewsMapper;
    @Resource
    GreenTextScan greenTextScan;
    @Resource
    GreenImageScan greenImageScan;
    @Resource
    FileStorageService fileStorageService;
    @Resource
    IArticleClient iArticleClient;
    @Resource
    WmChannelMapper wmChannelMapper;
    @Resource
    WmUserMapper wmUserMapper;
    @Resource
    WmSensitiveMapper wmSensitiveMapper;
    @Resource
    Tess4jClient tess4jClient;

    @Override
    @Async      //开启异步调用
    public void AutoScan(Integer id, Boolean test) {
        //1.查询文章
        WmNews news = wmNewsMapper.selectById(id);
        if (news == null) {
            throw new RuntimeException("WmNewsAutoScanImpl-----文章不存在");
        }
        if (!news.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            throw new RuntimeException("WmNewsAutoScanImpl-----该文章不需要审核");
        }
        if (test) {
            justATest(news);
            return;
        }
        //提取文本和图片
        Map<String, Object> textAndImages = handleTextAndImages(news);
        //1.进行自定义的敏感词审核DFA算法
        boolean haveSensitive = handleSensitiveScan((String) textAndImages.get("text"), news);
        if (haveSensitive) return;
        //2.审核文本（阿里云接口）
        boolean IsTextOk = textScan((String) textAndImages.get("text"), news);
        if (!IsTextOk) return;
        //3.审核图片（阿里云接口）
        boolean IsImagesOk = ImageScan((List<String>) textAndImages.get("images"), news);
        if (!IsImagesOk) return;
        //4.保存文章信息
        ResponseResult result = saveAppArticle(news);
        if (result.getCode() != 200) {
            throw new RuntimeException("WmNewsAutoScanImpl------保存文章时出错");
        }
        news.setArticleId((Long) result.getData());
        updateNews(news, WmNews.Status.PUBLISHED, "审核成功");  //审核成功  自动上架
    }

    private void justATest(WmNews news) {  //只能对文本进行自定义检测，图片也只是提取文字后检测
        //提取文本和图片
        Map<String, Object> textAndImages = handleTextAndImages(news);
        StringBuilder text = new StringBuilder((String) textAndImages.get("text"));
        List<String> images = (List<String>) textAndImages.get("images");
        images  = images.stream().distinct().collect(Collectors.toList());
        try {
            //下载图片字节数组
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);
                //进行图片文字识别
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                String imgText = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                text.append("-").append(imgText);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //1.进行自定义的敏感词审核DFA算法
        boolean haveSensitive = handleSensitiveScan(text.toString(), news);
        if (haveSensitive) return;
        //保存文章
        ResponseResult result = saveAppArticle(news);
        if (result.getCode() != 200) {
            throw new RuntimeException("WmNewsAutoScanImpl------保存文章时出错");
        }
        news.setArticleId((Long) result.getData());
        updateNews(news, WmNews.Status.PUBLISHED, "审核成功");
        return;
    }

    /**
     * 进行自定义的敏感词检测
     * @param text  文本
     * @param news  文章
     * @return  是否有敏感词
     */
    private boolean handleSensitiveScan(String text, WmNews news) {
        List<String> collect = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery()
                .select(WmSensitive::getSensitives))
                .stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        SensitiveWordUtil.initMap(collect);
        Map<String, Integer> map = SensitiveWordUtil.matchWords(text);
        if (map.size()>0){
            updateNews(news,WmNews.Status.FAIL,"文章内容违规"+map);
            return true;
        }
        return false;
    }

    /**
     * 远程调用保存文章数据
     *
     * @param news 文章数据
     */
    private ResponseResult saveAppArticle(WmNews news) {
        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(news, dto);
        //文章布局、频道、作者、id、创建时间
        dto.setLayout(news.getType());
        WmChannel channel = wmChannelMapper.selectById(news.getChannelId());
        if (channel != null) {
            dto.setChannelName(channel.getName());
        }
        dto.setAuthorId(news.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(news.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }
        if (news.getArticleId() != null) {
            dto.setId(news.getArticleId());
        }
        dto.setCreatedTime(new Date());
        return iArticleClient.saveArticle(dto);
    }

    /**
     * 图片信息审核
     *
     * @param images 图片列表
     * @param news   文章
     * @return 是否通过
     */
    private boolean ImageScan(List<String> images, WmNews news) {
        boolean flag = true;
        if (images.isEmpty()) {
            return flag;
        }
        //图片去重
        images = images.stream().distinct().collect(Collectors.toList());
        List<byte[]> list = new ArrayList<>();
        try {
            //下载图片字节数组
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);
                //进行图片文字识别
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                String text = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                boolean haveSensitive = handleSensitiveScan(text, news);
                if (haveSensitive) return false;
                list.add(bytes);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //请求接口
        try {
            Map map = greenImageScan.imageScan(list);
            if (map == null) throw new RuntimeException("greenImageScan----服务异常");
            if (map.get("suggestion").equals("block")) {
                flag = false;
                updateNews(news, WmNews.Status.FAIL, "图片内容违规");
            }
            if (map.get("suggestion").equals("block")) {
                flag = false;
                updateNews(news, WmNews.Status.ADMIN_AUTH, "文章图片存在不确定元素，请等待人工复审");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 文本信息检查
     *
     * @param text 文本
     * @return 是否成功
     */
    private boolean textScan(String text, WmNews news) {
        boolean flag = true;
        if (StringUtils.isBlank(text)) {
            return flag;
        }
        try {
            Map map = greenTextScan.greeTextScan(text);
            if (map == null) throw new RuntimeException("greenTextScan----服务异常");
            if (map.get("suggestion").equals("block")) {
                flag = false;
                updateNews(news, WmNews.Status.FAIL, "文章内容违规");
            }
            if (map.get("suggestion").equals("block")) {
                flag = false;
                updateNews(news, WmNews.Status.ADMIN_AUTH, "文章内容存在不确定元素，请等待人工复审");
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
            return flag;
        }
        return flag;
    }

    /**
     * 检查后修改文章内容
     *
     * @param news      文章
     * @param adminAuth 状态
     * @param reason    原因
     */
    private void updateNews(WmNews news, WmNews.Status adminAuth, String reason) {
        news.setStatus(adminAuth.getCode());
        news.setReason(reason);
        wmNewsMapper.updateById(news);
    }

    /**
     * 提取文章文本和图片
     *
     * @param news 文章
     * @return 文本和图片
     */
    private Map<String, Object> handleTextAndImages(WmNews news) {
        Map<String, Object> result = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();  //存贮文本
        //审核标题
        stringBuilder.append(news.getTitle());
        //审核标签
        stringBuilder.append(news.getLabels());
        List<String> images = new ArrayList<>();
        if (StringUtils.isNotBlank(news.getContent())) {
            List<Map> maps = JSON.parseArray(news.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                }
                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }
        if (StringUtils.isNotBlank(news.getImages())) {
            String[] split = news.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }
        result.put("text", stringBuilder.toString());
        result.put("images", images);
        return result;

    }
}
