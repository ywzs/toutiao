package com.heima.user.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.UserAuthDto;
import com.heima.user.service.ApUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/auth")
public class ApUserAuthController {
    @Resource
    ApUserService apUserService;

    @PostMapping("/list")
    public ResponseResult list(@RequestBody UserAuthDto dto){
        return apUserService.listAuth(dto);
    }
}
