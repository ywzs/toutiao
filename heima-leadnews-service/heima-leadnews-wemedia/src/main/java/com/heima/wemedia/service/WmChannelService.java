package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmChannelDto;
import com.heima.model.wemedia.dtos.WmSensitiveSearchDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {
    /**
     * 查询所有频道
     * @return
     */
    public ResponseResult findAll();

    /**
     * 通过id删除
     * @param id
     * @return
     */
    ResponseResult delById(Integer id);

    /**
     * 条件查询
     * @param dto
     * @return
     */
    ResponseResult listByCondition(WmSensitiveSearchDto dto);

    /**
     * 频道保存或者修改
     * @param adChannel
     * @return
     */
    ResponseResult saveOrUpdateCh(WmChannelDto adChannel);
}