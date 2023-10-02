package com.heima.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.threadLocal.appThreadLocalUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


@Service
@Transactional
@Log4j2
public class ApUserSearchServiceImpl  implements ApUserSearchService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    @Async
    public void insert(String keyword, Integer userId) {
        //1.查询当前用户的搜索关键词
        Query query = Query.query(Criteria.where("userId").is(userId).and("keyword").is(keyword));
        ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);
        //2.遍历，看是否存在，存在就更新搜索时间
        if (apUserSearch!=null){
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);    //有id就是修改，否则新增
            return;
        }
        //3.不存在就添加（已满就覆盖最旧的一次）
        apUserSearch  = new ApUserSearch();
        apUserSearch.setCreatedTime(new Date());
        apUserSearch.setUserId(userId);
        apUserSearch.setKeyword(keyword);
        Query query1 = Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdTime"));
        List<ApUserSearch> list = mongoTemplate.find(query1, ApUserSearch.class);
        if (list.size()>=10){
            ApUserSearch lastUserSearch = list.get(list.size() - 1);
            mongoTemplate.findAndReplace(Query.query(Criteria.where("id").is(lastUserSearch.getId())),apUserSearch);
        }else {
            mongoTemplate.save(apUserSearch);
        }
    }

    @Override
    public ResponseResult loadHistory() {
        ApUser user = appThreadLocalUtil.getUser();
        if (user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Query query = Query.query(Criteria.where("userId").is(user.getId()))
                .with(Sort.by(Sort.Direction.DESC, "createdTime"));
        List<ApUserSearch> apUserSearches = mongoTemplate.find(query, ApUserSearch.class);
        return ResponseResult.okResult(apUserSearches);
    }

    @Override
    public ResponseResult delHistory(HistorySearchDto dto) {
        if (StringUtils.isBlank(dto.getId())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUser user = appThreadLocalUtil.getUser();
        if (user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Query query = Query.query(Criteria.where("userId").is(user.getId()).and("id").is(dto.getId()));
        mongoTemplate.remove(query,ApUserSearch.class);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}