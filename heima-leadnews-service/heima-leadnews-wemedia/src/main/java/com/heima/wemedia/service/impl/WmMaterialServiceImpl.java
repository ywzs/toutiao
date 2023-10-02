package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.threadLocal.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    @Autowired
    FileStorageService fileStorageService;

    /**
     * 素材图片上传
     * @param multipartFile  图片
     */
    @Override
    public ResponseResult uploadPic(MultipartFile multipartFile) {
        //判断图片是否存在
        if (multipartFile == null || multipartFile.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.上传图片
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = multipartFile.getOriginalFilename();
        String back = filename.substring(filename.lastIndexOf("."));
        String imgUrl = null;
        try {
            imgUrl = fileStorageService.uploadImgFile("", uuid + back, multipartFile.getInputStream());
            log.info("上传图片成功---->{}", imgUrl);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("文件上传失败，请查看错误信息");
        }
        //2.返回访问地址
        WmMaterial material = new WmMaterial();
        material.setUserId(WmThreadLocalUtil.getUser().getApUserId());
        material.setUrl(imgUrl);
        material.setIsCollection((short) 0);
        material.setType((short) 0);
        material.setCreatedTime(new Date());
        save(material);
        return ResponseResult.okResult(material);
    }
    /**
     * 素材展示
     * @param dto 数据
     * @return 结果
     */
    @Override
    public ResponseResult listPic(WmMaterialDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmMaterial> wrapper = new LambdaQueryWrapper<>();
        Short collection = dto.getIsCollection();
        //封装是否收藏
        if (collection!=null && collection==1){
            wrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        //封住用户信息
        wrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getApUserId())
                .orderByDesc(WmMaterial::getCreatedTime);
        page = page(page, wrapper);

        ResponseResult result =  new PageResponseResult(dto.getPage(),dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());
        return result;
    }
}
