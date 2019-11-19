package com.miny.engine;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.miny.dto.EsConf;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
// version 6.4.1
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
/*
// version 7.4.2
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.action.support.IndicesOptions;
*/
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author huangwm@2345.com
 * @date 2019/11/19
 */
@Service
public class EsEngine {
    @Autowired
    EsConf esConf;
    private static Logger logger = LoggerFactory.getLogger(EsEngine.class);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Calendar calendar = Calendar.getInstance();

    /**
     * 索引资讯的日期范围
     */
    private final int limit_day_num = 1000;

    /**
     * 获取elastic search rest client
     * @return RestHighLevelClient
     */
    private RestHighLevelClient getEsRestClient() {
        List<HttpHost> httpHosts = new ArrayList<HttpHost>();
        for (String host : esConf.getHost()) {
            HttpHost httpHost = new HttpHost(host, esConf.getPort(), esConf.getSchema());
            httpHosts.add(httpHost);
        }
        return new RestHighLevelClient(
            RestClient.builder((HttpHost[])httpHosts.toArray(new HttpHost[0])));
    }

    /**
     * 判断索引是否存在
     * @param indexNames 索引列表
     * @return Boolean
     */
    public Boolean existsIndex(String[] indexNames) {
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(indexNames);
        getIndexRequest.local(false);
        getIndexRequest.humanReadable(true);
        getIndexRequest.includeDefaults(true);
        //getIndexRequest.indicesOptions(IndicesOptions);
        Boolean ret = false;
        try{
            ret = getEsRestClient().indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            logger.warn("Judge the existance of index failed!!!");
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 获取索引信息 
     * @param indexName 索引名称
     * @return Map<String, String>
     */
    public Map<String, String> getIndexInfo(String indexName) {
        if (!existsIndex(new String[]{indexName})) {
            logger.warn("Index '{}' not existed, cannot it's info!!!", indexName);
            return new HashMap<>();
        }
        Map<String, String> indexInfo = new HashMap<String, String>();
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(indexName);
        getIndexRequest.includeDefaults(true);
        try{
            GetIndexResponse getIndexResponse = getEsRestClient().indices().get(
                getIndexRequest, RequestOptions.DEFAULT);
            Set<String> infoKeys = getIndexResponse.getSettings().get(indexName).keySet();
            for (String infoKey : infoKeys) {
                String value = getIndexResponse.getSetting(indexName, infoKey);
                if (infoKey.equals("index.creation_date")) {
                    Timestamp ts = new java.sql.Timestamp(Long.valueOf(value));
                    value = ts.toString();
                }
                indexInfo.put(infoKey, value);
            }
        } catch (Exception e) {
            logger.warn("Get index '{}' info failed!!!", indexName);
            e.printStackTrace();
        }
        return indexInfo;
    }

    /**
     * 创建索引
     * @param indexName 索引名称
     * @param shardNum 分片数
     * @param replicNum 备份数
     * @param builder 属性builder
     * @return Boolean
     */
    public Boolean createIndex(String indexName,
                               int shardNum,
                               int replicNum,
                               XContentBuilder builder) {
        if (existsIndex(new String[]{indexName})) {
            logger.warn("Index '{}' existed, cannot create it!!!", indexName);
            return false;
        }
        RestHighLevelClient client = getEsRestClient();
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
            .put("index.number_of_shards", Integer.toString(shardNum))
            .put("index.number_of_replicas", Integer.toString(replicNum)));
        //request.mapping(builder); // version 7.4.2
        request.mapping("type", builder); // version 6.4.1
        CreateIndexResponse response;
        boolean ret = false;
        try {
            response = client.indices().create(request, RequestOptions.DEFAULT);
            ret = response.isAcknowledged();
        } catch (Exception e) {
            logger.error("Create index {} failed!!!", indexName);
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 批量索引文档
     * @param indexName 索引名称
     * @param docList 文档列表
     * @return Boolean
     */
    public Boolean indexDocs(String indexName,
                             List<String> docList) {
        if (!existsIndex(new String[]{indexName})) {
            logger.warn("Index '{}' not existed, cannot index it!!!", indexName);
            return false;
        }
        if (docList.size() == 0) {
            logger.warn("There is no doc in docList!");
            return false;
        }
        BulkRequest bulkRequest = new BulkRequest();
        Iterator<String> iter = docList.iterator();
        Boolean ret = false;
        while (iter.hasNext()) {
            String jsonString = iter.next();
            //IndexRequest indexRequest = new IndexRequest(indexName) // version 7.4.2
            IndexRequest indexRequest = new IndexRequest(indexName, "type") // version 6.4.1
                .source(jsonString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        try {
            BulkResponse bulkResponse = getEsRestClient().bulk(bulkRequest, RequestOptions.DEFAULT);
            ret = !bulkResponse.hasFailures();
            if (!ret) {
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        logger.warn(failure.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("index docs failed!!!");
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 删除索引
     * @param indexName 索引名称
     * @return Boolean
     */
    public Boolean deleteIndex(String indexName) {
        if (!existsIndex(new String[]{indexName})) {
            return true;
        }
        RestHighLevelClient client = getEsRestClient();
        DeleteIndexRequest deleteRequest = new DeleteIndexRequest(indexName);
        deleteRequest.timeout(TimeValue.timeValueSeconds(1));
        //deleteRequest.indicesOptions(IndicesOptions.lenientExpand());
        Boolean ret = false;
        try {
            AcknowledgedResponse deleteResponse = client.indices().delete(
                deleteRequest, RequestOptions.DEFAULT);
            ret = deleteResponse.isAcknowledged();
        } catch (IOException e) {
            logger.error("Delete index {} failed!!!", indexName);
            e.printStackTrace();
        }
        return ret;
    }

    /**  */
    /**
     * 搜索资讯
     * @param indexNames 索引列表
     * @param keyword 搜索词
     * @param fieldNames 搜索域
     * @param pageNum 展示页数
     * @param pageSize 每页展示的资讯数
     * @return ArrayList<Map<String, Object>>
     */
    public ArrayList<Map<String, Object>> searchDocs(String[] indexNames,
                                                     String keyword,
                                                     String[] fieldNames,
                                                     int pageNum,
                                                     int pageSize) {
        if (!existsIndex(indexNames)) {
            logger.warn("Index not existed, cannot search it!!!");
            return new ArrayList<>();
        }
        if (!existsIndex(indexNames)) return new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest(indexNames);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(multiMatchSearch(keyword, fieldNames));
        boolQueryBuilder = fixMatchAndFilter(boolQueryBuilder);
        searchSourceBuilder.highlighter(getHighlightBuilder());
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.from((pageNum - 1) * pageSize);
        searchSourceBuilder.size(pageSize);
        //searchRequest.routing("routing");
        searchRequest.source(searchSourceBuilder);
        // 搜索
        ArrayList<Map<String, Object>> resultList = new ArrayList<>();
        try {
            SearchResponse searchResponse = getEsRestClient().search(
                searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit searchHit : searchHits) {
                Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
                Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
                for (String fieldName : fieldNames) {
                    HighlightField hField = highlightFields.get(fieldName);
                    if (hField != null) {
                        //String hText = "";
                        StringBuffer hText = new StringBuffer();
                        Text[] fragments = hField.fragments();
                        for (Text text : fragments) {
                            hText.append(text.toString());
                        }
                        sourceAsMap.put(fieldName, hText);
                    }
                }
                resultList.add(sourceAsMap);
            }
        } catch (Exception e) {
            logger.error("Search keyword: {} in index failed!!!", keyword);
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 查询搜索到的数量
     * @param indexNames 索引名称列表
     * @param keyword 搜索词
     * @param fieldNames 搜索域
     * @return Long
     */
    /* version 7.4.2
    public Long getSearchCount(String[] indexNames, String keyword, String[] fieldNames) {
        Long count = 0L;
        CountRequest countRequest = new CountRequest();
        CountResponse countResponse;
        countRequest.indices(indexNames);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (keyword.length() == 0) {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        } else {
            boolQueryBuilder.must(multiMatchSearch(keyword, fieldNames));
        }
        boolQueryBuilder = fixMatchAndFilter(boolQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);
        countRequest.source(searchSourceBuilder);
        try {
            countResponse = getEsRestClient().count(countRequest, RequestOptions.DEFAULT);
            count = countResponse.getCount();
        } catch (IOException e) {
            logger.error("get keyword: {} search count in index {} failed!!!", keyword, indexNames);
            e.printStackTrace();
        }
        return count;
    }
    */

    /**
     * 创建多匹配builder
     * @param keyword 搜索词
     * @param fieldNames 搜索域
     * @return MultiMatchQueryBuilder
     */
    private MultiMatchQueryBuilder multiMatchSearch(String keyword, String[] fieldNames) {
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders
            .multiMatchQuery(keyword, fieldNames)
            .operator(Operator.AND);
        //multiMatchQueryBuilder.fuzziness(Fuzziness.AUTO);
        return multiMatchQueryBuilder;
    }

    /**
     * 固定匹配和过滤(每次搜索都会应用的匹配和过滤规则)
     * @return BoolQueryBuilder 
     */
    private BoolQueryBuilder fixMatchAndFilter(BoolQueryBuilder boolQueryBuilder) {
        // 日期过滤
        Date endDate = new Date();
        Date startDate = getDate(endDate, -limit_day_num);
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("dt").gte(dateFormat.format(startDate)));
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("dt").lte(dateFormat.format(endDate)));
        return boolQueryBuilder;
    }

    /**
     * 创建高亮显示的builder
     * @return HighlightBuilder
     */
    private HighlightBuilder getHighlightBuilder() {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field hTitle = new HighlightBuilder.Field("title");
        HighlightBuilder.Field highlightContent = new HighlightBuilder.Field("content");
        highlightBuilder.field(hTitle);
        highlightBuilder.field(highlightContent);
        highlightBuilder.preTags("<span style=color:red>").postTags("</span>");
        return highlightBuilder;
    }

    /**
     * 获取某一天的时间
     * @param now 当前日期
     * @param offset_day 与now相差的天数
     * @return Date
     */
    private Date getDate(Date now, int offset_day) {
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, offset_day);
        Date targetDate = calendar.getTime();
        return targetDate;
    }
}
