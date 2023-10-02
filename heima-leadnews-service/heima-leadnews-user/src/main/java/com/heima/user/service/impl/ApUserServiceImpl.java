package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional   //(ctrl + i) 快速实现方法
@Slf4j
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    @Override
    public ResponseResult login(LoginDto loginDto) {
        String password = loginDto.getPassword();
        //1.用户登录有电话和密码；
        if (StringUtils.isNotBlank(loginDto.getPhone()) && StringUtils.isNotBlank(password)) {
            ApUser apUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, loginDto.getPhone()));
            if (apUser == null) {
                //没有该用户
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户信息不存在");
            }
            //2.比对加密密码
            String salt = apUser.getSalt();
            String TPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes(StandardCharsets.UTF_8));
            if (!apUser.getPassword().equals(TPassword)){
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }
            //3.返回jwt令牌
            String token = AppJwtUtil.getToken(apUser.getId().longValue());
            Map<String,Object> map = new HashMap<>();
            apUser.setSalt("");
            apUser.setPassword("");
            map.put("user",apUser);
            map.put("token",token);
            return ResponseResult.okResult(map);
        }
        //2.游客登录(以0为id生成token)
        Map<String,Object> map = new HashMap<>();
        map.put("token",AppJwtUtil.getToken(0L));
        return ResponseResult.okResult(map);
    }
}
