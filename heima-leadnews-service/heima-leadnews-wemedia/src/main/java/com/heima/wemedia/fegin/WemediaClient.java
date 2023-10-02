package com.heima.wemedia.fegin;

import com.heima.apis.wemedia.IWemediaClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class WemediaClient implements IWemediaClient {
    @Resource
    WmChannelService wmChannelService;

    @Override
    @GetMapping("/api/v1/channel/listChannel")
    public ResponseResult findAll() {
        return wmChannelService.findAll();
    }
}
