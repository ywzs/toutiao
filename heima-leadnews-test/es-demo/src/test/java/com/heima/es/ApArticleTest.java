package com.heima.es;

import com.alibaba.fastjson.JSON;
import com.heima.es.mapper.ApArticleMapper;
import com.heima.es.pojo.SearchArticleVo;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ApArticleTest {
    @Resource
    ApArticleMapper apArticleMapper;
    @Resource
    RestHighLevelClient restHighLevelClient;

    /**
     * 注意：数据量的导入，如果数据量过大，需要分页导入
     * @throws Exception
     */
    @Test
    public void init() throws Exception {
        List<SearchArticleVo> articleVos = apArticleMapper.loadArticleList();
        //批量导入文章数据
        BulkRequest bulkRequest = new BulkRequest("app_info_article");
        for (SearchArticleVo articleVo : articleVos) {
            IndexRequest indexRequest = new IndexRequest().id(articleVo.getId().toString())
                    .source(JSON.toJSONString(articleVo), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
    }

}