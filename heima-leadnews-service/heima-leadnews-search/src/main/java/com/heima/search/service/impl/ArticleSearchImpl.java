package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.vos.SearchArticleVo;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.threadLocal.appThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.heima.common.Constant.ArticleConstant.ARTICLE_ES_BASE_NAME;

@Service
@Slf4j
public class ArticleSearchImpl implements ArticleSearchService {
    @Resource
    RestHighLevelClient restHighLevelClient;
    @Resource
    ApUserSearchService apUserSearchService;
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {
        //1.检查参数
        if (dto==null || StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //5.新增用户搜索记录
        ApUser user = appThreadLocalUtil.getUser();
        if (user!=null && dto.getFromIndex()==0){
            apUserSearchService.insert(dto.getSearchWords(),user.getId());
        }

        //2.设置查询条件
        SearchRequest searchRequest = new SearchRequest("app_info_article");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        //2.1关键词分词查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords())
                .field("title").field("content").defaultOperator(Operator.OR);   //需要在2个域中匹配
        boolQueryBuilder.must(queryBuilder);      //必须

        //2.3发布时间查询
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
        boolQueryBuilder.filter(rangeQueryBuilder);

        //2.2分页查询
        sourceBuilder.from(0).size(dto.getPageSize());
        //2.4倒序查询发布时间
        sourceBuilder.sort("publishTime", SortOrder.DESC);

        //3.设置高亮(高亮字段，高亮标签，高亮反标签)
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
        highlightBuilder.postTags("</font>");
        sourceBuilder.highlighter(highlightBuilder);

        //4.请求es服务
        sourceBuilder.query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //4.封装返回结果
        List<Map> list = new ArrayList<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            Map map = JSON.parseObject(hit.getSourceAsString(), Map.class);
            //处理高亮
            if(hit.getHighlightFields()!=null && hit.getHighlightFields().size()>0){
//                if (hit.getHighlightFields().get("title")==null)return ResponseResult.okResult(list);
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String join = StringUtils.join(titles);
                //高亮
                map.put("h_title",join);
            }else {
                //原始
                map.put("h_title",map.get("title"));
            }
            list.add(map);
        }

        return ResponseResult.okResult(list);
    }

    /**
     * 同步文章索引库
     * @param text 索引信息
     */
    @Override
    public void syncIndex(String text) {
        IndexRequest indexRequest = new IndexRequest(ARTICLE_ES_BASE_NAME);
        SearchArticleVo searchArticleVo = JSON.parseObject(text, SearchArticleVo.class);
        indexRequest.id(searchArticleVo.getId().toString());
        indexRequest.source(text);
        try {
            restHighLevelClient.index(indexRequest,RequestOptions.DEFAULT);
            log.info("同步文章索引{}成功",searchArticleVo.getId());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("同步消息失败，请管理员手动查看+文章id{}",searchArticleVo.getId());
        }
    }
}
