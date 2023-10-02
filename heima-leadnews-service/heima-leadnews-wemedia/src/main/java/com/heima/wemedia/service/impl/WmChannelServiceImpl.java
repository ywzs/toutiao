package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmChannelDto;
import com.heima.model.wemedia.dtos.WmSensitiveSearchDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {
    @Resource
    WmNewsMapper wmNewsMapper;


    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }

    @Override
    public ResponseResult delById(Integer id) {
        if (id == null) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        WmChannel channel = getById(id);
        //启用状态 不能删除
        if (channel.getStatus()) return ResponseResult.errorResult(AppHttpCodeEnum.CAN_NOT_DEL);
        return removeById(id) ? ResponseResult.okResult(AppHttpCodeEnum.SUCCESS) : ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }

    @Override
    public ResponseResult listByCondition(WmSensitiveSearchDto dto) {
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(dto.getName()), WmChannel::getName, dto.getName())
                .orderByDesc(WmChannel::getCreatedTime);
        page = page(page, wrapper);
        ResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());
        return result;
    }

    @Override
    public ResponseResult saveOrUpdateCh(WmChannelDto adChannel) {
        if (StringUtils.isBlank(adChannel.getName())) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        WmChannel channel = lambdaQuery().eq(WmChannel::getName, adChannel.getName()).one();
        if (channel != null) return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST);
        channel = new WmChannel();
        BeanUtils.copyProperties(adChannel, channel);
        if (channel.getCreatedTime() == null) channel.setCreatedTime(new Date());
        if (adChannel.getId()==null){
            //保存操作
            return save(channel)?ResponseResult.okResult(AppHttpCodeEnum.SUCCESS):ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
        }
        //修改操作
        if (!channel.getStatus()){
            //禁用(用引用就不能)
            LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WmNews::getChannelId,channel.getId());
            WmNews news = wmNewsMapper.selectOne(wrapper);
            if (news!=null){   //有引用
                return ResponseResult.errorResult(AppHttpCodeEnum.CAN_NOT_UPDATE);
            }
        }
        return updateById(channel)?ResponseResult.okResult(AppHttpCodeEnum.SUCCESS):ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }
}