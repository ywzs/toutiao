package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApAssociateWordsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/associate")
public class ApUserAssociateController {
    @Resource
    ApAssociateWordsService apAssociateWordsService;
    @PostMapping("/search")
    public ResponseResult load(@RequestBody UserSearchDto dto){
        return apAssociateWordsService.load(dto);
    }
}
