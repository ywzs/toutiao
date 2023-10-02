package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.service.ApUserSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("api/v1/history")
public class ApUserSearchController {
    @Resource
    ApUserSearchService apUserSearchService;
    //加载用户历史记录
    @PostMapping("/load")
    public ResponseResult load(){
        return apUserSearchService.loadHistory();
    }
    @PostMapping("/del")
    public ResponseResult del(@RequestBody HistorySearchDto dto){
        return apUserSearchService.delHistory(dto);
    }
}
