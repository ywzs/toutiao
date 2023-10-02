package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/material")
public class WmFileController {
    @Resource
    WmMaterialService wmMaterialService;

    @PostMapping("/upload_picture")
    public ResponseResult uploadPic(MultipartFile multipartFile){
        return wmMaterialService.uploadPic(multipartFile);
    }

    @PostMapping("/list")
    public ResponseResult listPic(@RequestBody WmMaterialDto dto){
        return wmMaterialService.listPic(dto);
    }
}
