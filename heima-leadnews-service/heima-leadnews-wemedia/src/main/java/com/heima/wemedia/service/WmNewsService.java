package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {
    /**
     * 条件查询文章列表
     * @param dto  条件
     * @return  文章列表
     */
    public ResponseResult findList(WmNewsPageReqDto dto);

    /**
     *  保存、新增文章
     * @param dto  数据
     * @return 是否成功
     */
    public ResponseResult submitNews(WmNewsDto dto);

    /**
     * 上架或者下架文章
     * @param dto 数据
     * @return  消息
     */
    public ResponseResult downOrUp(WmNewsDto dto);
}
