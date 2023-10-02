package com.heima.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.pojos.ApAssociateWords;
import com.heima.search.service.ApAssociateWordsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 联想词表 服务实现类
 * </p>
 *
 * @author itheima
 */
@Slf4j
@Service
public class ApAssociateWordsServiceImpl implements ApAssociateWordsService {
    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult load(UserSearchDto dto) {
        if (StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //最多查询20条数据
        if (dto.getPageSize()>20){
            dto.setPageSize(20);
        }
        Query query = Query.query(Criteria.where("associateWords")
                .regex(".*?\\" + dto.getSearchWords() + ".*")).limit(dto.getPageSize());
        List<ApAssociateWords> apAssociateWords = mongoTemplate.find(query, ApAssociateWords.class);
        return ResponseResult.okResult(apAssociateWords);
    }


















}