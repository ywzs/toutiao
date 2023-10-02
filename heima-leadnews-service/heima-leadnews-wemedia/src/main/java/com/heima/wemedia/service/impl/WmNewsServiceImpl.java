package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.Constant.WeMediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.threadLocal.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScan;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.heima.common.Constant.WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Resource
    WmNewsMaterialMapper wmNewsMaterialMapper;
    @Resource
    WmMaterialMapper wmMaterialMapper;
    @Resource
    WmNewsTaskService wmNewsTaskService;
    @Resource
    KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 条件查询文章列表
     *
     * @param dto 条件
     * @return 文章列表
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();
        //频道，状态、时间、关键字模糊查询、用户id、倒叙
        wrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId())
                .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                .between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null,
                        WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate())
                .like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword())
                .eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getApUserId())
                .orderByDesc(WmNews::getPublishTime);

        page = page(page, wrapper);
        ResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());
        return result;
    }

    /**
     * 保存、新增文章
     *
     * @param dto 数据
     * @return 是否成功
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.保存或修改文章
        WmNews news = new WmNews();
        BeanUtils.copyProperties(dto, news);
        //封面图片从list转string
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            news.setImages(StringUtils.join(dto.getImages(), ","));
        }
        //封面类型
        if (dto.getType().equals(WeMediaConstants.WM_NEWS_TYPE_AUTO)) {
            news.setType(null);
        }
        saveOrUpdateNews(news);
        //2.判断是否为草稿
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        //3.不是草稿就需要保存文章与素材的关系
        //提取图片Url
        List<String> materials = ectractUrl(dto.getContent());
        saveRelativeInfoContent(materials, news.getId());
        //4.不是草稿保存封面图片与素材的关系
        saveRelativeInfoCover(dto, news, materials);

        //5.保存文章就调用异步方法进行自动审核
        //wmNewsAutoScan.AutoScan(news.getId(),true);
        //6.加入延时队列进行处理
        wmNewsTaskService.addNewsToTask(news.getId(), news.getPublishTime());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * 如果封面类型为自动就要根据匹配规则配备封面
     * 匹配规则：1到3 --->单图 type=1
     * 大于3   ---->多图 type=3
     * 无图   ---->type = 0
     *
     * @param dto       数据
     * @param news      文章
     * @param materials 材料
     */
    private void saveRelativeInfoCover(WmNewsDto dto, WmNews news, List<String> materials) {
        List<String> images = dto.getImages();
        if (dto.getType().equals(WeMediaConstants.WM_NEWS_TYPE_AUTO)) {
            //需要匹配规则
            if (materials.size() >= 3) {
                //多图
                news.setType(WeMediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1 && materials.size() < 3) {
                //单图
                news.setType(WeMediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {  //无图
                news.setType(WeMediaConstants.WM_NEWS_NONE_IMAGE);
            }
            //修改文章
            if (images != null && images.size() > 0) {
                news.setImages(StringUtils.join(materials, ","));
            }
        }
        if (images != null && images.size() > 0) {
            saveRelativeInfo(materials, news.getId(), WeMediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 保存文章图片与素材的关系
     *
     * @param materials 图片列表
     * @param id        文章id
     */
    private void saveRelativeInfoContent(List<String> materials, Integer id) {
        saveRelativeInfo(materials, id, WeMediaConstants.WM_CONTENT_REFERENCE);
    }

    /**
     * 保存文章图片与素材的关系
     *
     * @param type 0 表示内容引用   1  表示封面引用
     */
    private void saveRelativeInfo(List<String> materials, Integer id, Short type) {
        if (materials != null && !materials.isEmpty()) {
            //查询数据库获取素材数据
            List<WmMaterial> dbMaterials = wmMaterialMapper
                    .selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));
            if (dbMaterials == null || dbMaterials.size() == 0 || materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFORENCE_FAIL);
            }
            List<Integer> urlIds = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            wmNewsMaterialMapper.saveRelations(urlIds, id, type);
        }
    }

    /**
     * 提取内容中的图片信息
     *
     * @param content 内容
     * @return list
     */
    private List<String> ectractUrl(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                materials.add((String) map.get("value"));
            }
        }
        return materials;
    }

    /**
     * 保存或者修改文章
     *
     * @param news 文章
     */
    private void saveOrUpdateNews(WmNews news) {
        //补全数据
        news.setUserId(WmThreadLocalUtil.getUser().getApUserId());
        news.setCreatedTime(new Date());
        news.setSubmitedTime(new Date());
        news.setEnable((short) 1);
        if (news.getId() == null) {
            //保存
            save(news);
        } else {
            //修改(先删除文章与素材的关联)
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery()
                    .eq(WmNewsMaterial::getNewsId, news.getId()));
            updateById(news);
        }
    }

    /**
     * Topic默认8个分区，相同属性的业务数据推送至同一Topic中，Topic命名规范如下：
     * 1.Topic_业务名/业务表名
     * 2.英文字母统一小写
     * 3.短横杠"-"以下划线代替 " _ "
     *
     * @param dto 数据
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        if (dto.getId() == null || dto.getEnable() == null || dto.getEnable() < 0 || dto.getEnable() > 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            //文章失效
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不是发布状态，不能上下架");
        }
        wmNews.setEnable(dto.getEnable());
        updateById(wmNews);
        dto.setArticleId(wmNews.getArticleId());
        //异步执行--->向kafka发送消息
        kafkaTemplate.send(WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(dto));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
