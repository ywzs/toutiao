package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {
    /**
     * 素材图片上传
     * @param multipartFile  图片
     */
    public ResponseResult uploadPic(MultipartFile multipartFile);

    /**
     * 素材展示
     * @param dto 数据
     * @return 结果
     */
    public ResponseResult listPic(WmMaterialDto dto);

}