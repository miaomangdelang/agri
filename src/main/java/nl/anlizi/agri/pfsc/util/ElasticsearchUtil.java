package nl.anlizi.agri.pfsc.util;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zheos.elasticsearch.config.ElasticsearchProperties;
import org.zheos.elasticsearch.model.entity.BulkRequestEntity;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Calendar.getInstance;

/**
 * es工具类
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/20 19:54
 */
@Component
public class ElasticsearchUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchUtil.class);

    @Resource(name = "restHighLevelClient")
    private RestHighLevelClient elasticsearchClient;

    @Resource
    private ElasticsearchProperties properties;

    public static final String ID = "id";

    public static final String LOCATION = "location";

    public static final String SCROLL_ID = "scrollId";

    /**
     * 查询数据
     *
     * @param index               索引名成
     * @param searchSourceBuilder 查询体构建类
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     * @throws IOException Elasticsearch查询异常
     */
    public List<Map<String, Object>> search(String index, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchResponse response = searchResponse(index, searchSourceBuilder);
        SearchHits hits = response.getHits();

        return Arrays.stream(hits.getHits()).map(this::transformSearchHit).collect(Collectors.toList());
    }

    /**
     * 查询返回结果
     *
     * @param index               索引名称
     * @param searchSourceBuilder 查询条件构造
     * @return {@link SearchResponse} elasticsearch查询结果
     * @throws IOException Elasticsearch查询异常
     */
    private SearchResponse searchResponse(String index, SearchSourceBuilder searchSourceBuilder) throws IOException {
        long beginTime = System.currentTimeMillis();
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        long endTime = System.currentTimeMillis();
        LOG.info("查询数据列表结束，耗时[{}]ms", endTime - beginTime);
        return response;
    }

    /**
     * 滚轴查询初始化
     *
     * @param index               索引
     * @param searchSourceBuilder 查询条件
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     * @throws IOException Elasticsearch查询异常
     */
    public List<Map<String, Object>> scrollInit(String index, SearchSourceBuilder searchSourceBuilder) throws IOException {
        // 设置scroll有效时间
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(3L));

        LOG.info("滚动查询初始化开始");
        SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        LOG.info("滚动查询初始化结束: scrollId=[{}], 数据总数=[{}], 耗时=[{}]", response.getScrollId(), response.getHits().getTotalHits().value, response.getTook());
        return scrollResult(response);
    }

    /**
     * scroll查询
     *
     * @param scrollId 滚轴查询id
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     * @throws IOException Elasticsearch查询异常
     */
    public List<Map<String, Object>> scrollSearch(String scrollId) throws IOException {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueMinutes(2L));
        SearchResponse response = elasticsearchClient.scroll(scrollRequest, RequestOptions.DEFAULT);
        LOG.info("滚动查询结束: 耗时=[{}], 数据总数=[{}], scrollId=[{}]", response.getTook(), response.getHits().getTotalHits().value, scrollId);

        return scrollResult(response);
    }

    /**
     * 清空scroll
     *
     * @param scrollId 滚轴查询id
     * @return boolean 成功返回true
     * @throws IOException Elasticsearch查询异常
     */
    public boolean scrollDelete(String scrollId) throws IOException {
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);

        ClearScrollResponse clearScrollResponse = elasticsearchClient.clearScroll(request, RequestOptions.DEFAULT);
        LOG.info("释放查询上下文, scrollId=[{}]", scrollId);
        return clearScrollResponse.isSucceeded();
    }

    /**
     * scroll查询结果处理
     *
     * @param response elasticsearch查询结果
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     */
    private List<Map<String, Object>> scrollResult(SearchResponse response) {
        SearchHits hits = response.getHits();
        List<Map<String, Object>> searchMap = Arrays.stream(hits.getHits()).map(this::transformSearchHit).collect(Collectors.toList());

        // 添加scrollId
        Map<String, Object> scrollIdHashMap = new HashMap<>(1);
        scrollIdHashMap.put(SCROLL_ID, response.getScrollId());
        searchMap.add(scrollIdHashMap);
        return searchMap;
    }

    /**
     * 将SearchHit转为 {@code Map<String, Object>}
     *
     * @param searchHit 单条查询结果
     * @return {@code Map<String, Object>} 查询结果键值对，包含数据id
     */
    private Map<String, Object> transformSearchHit(SearchHit searchHit) {
        Map<String, Object> dataMap = searchHit.getSourceAsMap();
        dataMap.put(ID, searchHit.getId());
        return dataMap;
    }

    /**
     * 统计索引总数
     *
     * @param index 索引名称
     * @return Long 总数
     * @throws IOException Elasticsearch查询异常
     */
    public Long count(String index) throws IOException {
        return count(index, null);
    }

    /**
     * 统计总数
     *
     * @param index               索引名称
     * @param searchSourceBuilder 查询体构建类
     * @return Long 总数
     * @throws IOException Elasticsearch查询异常
     */
    public Long count(String index, SearchSourceBuilder searchSourceBuilder) throws IOException {
        long beginTime = System.currentTimeMillis();
        CountRequest countRequest = new CountRequest(index);
        if (searchSourceBuilder != null) {
            countRequest.source(searchSourceBuilder);
        }
        CountResponse count = elasticsearchClient.count(countRequest, RequestOptions.DEFAULT);
        long endTime = System.currentTimeMillis();

        long totalNum = count.getCount();
        LOG.info("查询总数：count=[{}], 耗时[{}]ms", totalNum, endTime - beginTime);
        return totalNum;
    }

    /**
     * 根据给定时间聚合，也可以统计某个字段的和
     *
     * @param dateFlag  聚合时间类型
     * @param interval  时间间隔，当前时间往前间隔
     * @param timeField 时间聚合字段
     * @param sumField  求和字段
     * @return {@code List<AggregationDetail>} 聚合结果信息
     * @throws IOException Elasticsearch查询异常
     */
    public List<ElasticsearchUtil.AggregationDetail> dateHistogram(ElasticsearchUtil.DateFlagEnum dateFlag, int interval, String timeField, String sumField) throws IOException {
        String queryBeginDate, queryEndDate, queryBeginTime, queryEndTime, dateFormat;
        DateHistogramInterval intervalType;
        interval = -interval;

        switch (dateFlag) {
            case YEAR:
                intervalType = DateHistogramInterval.YEAR;
                dateFormat = ElasticsearchUtil.DateUtil.YEAR_FORMAT;
                queryBeginDate = ElasticsearchUtil.DateUtil.getDateByIntervalYear(interval);
                queryEndDate = ElasticsearchUtil.DateUtil.getDateByIntervalYear(0);
                queryBeginTime = ElasticsearchUtil.DateUtil.getQueryBeginTimeByYear(queryBeginDate);
                queryEndTime = ElasticsearchUtil.DateUtil.getQueryEndTimeByYear(queryEndDate);
                break;
            case MONTH:
                intervalType = DateHistogramInterval.MONTH;
                dateFormat = ElasticsearchUtil.DateUtil.MONTH_FORMAT;
                queryBeginDate = ElasticsearchUtil.DateUtil.getDateByIntervalMonth(interval);
                queryEndDate = ElasticsearchUtil.DateUtil.getDateByIntervalMonth(0);
                queryBeginTime = ElasticsearchUtil.DateUtil.getQueryBeginTimeByMonth(queryBeginDate);
                queryEndTime = ElasticsearchUtil.DateUtil.getQueryEndTimeByMonth(queryEndDate);
                break;
            default:
                intervalType = DateHistogramInterval.DAY;
                dateFormat = ElasticsearchUtil.DateUtil.DATE_FORMAT;
                queryBeginDate = ElasticsearchUtil.DateUtil.getDateByIntervalDay(interval);
                queryEndDate = ElasticsearchUtil.DateUtil.getDateByIntervalDay(0);
                queryBeginTime = ElasticsearchUtil.DateUtil.getQueryBeginTimeByDate(queryBeginDate);
                queryEndTime = ElasticsearchUtil.DateUtil.getQueryEndTimeByDate(queryEndDate);
                break;
        }

        // 封装过滤条件
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery().filter(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(timeField).gte(queryBeginTime).lte(queryEndTime)));
        // 时间聚合条件，内嵌求和
        DateHistogramAggregationBuilder aggregationBuilder = AggregationBuilders.dateHistogram(timeField).field(timeField).calendarInterval(intervalType).format(dateFormat).minDocCount(0).extendedBounds(new LongBounds(queryBeginDate, queryEndDate));
        if (StringUtils.isNotBlank(sumField)) {
            aggregationBuilder.subAggregation(AggregationBuilders.sum(sumField).field(sumField));
        }

        SearchRequest searchRequest = new SearchRequest(getIndex()).source(new SearchSourceBuilder().size(0).query(filterQuery).aggregation(aggregationBuilder));
        Histogram aggregation = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT).getAggregations().get(timeField);
        if (StringUtils.isNotBlank(sumField)) {
            return aggregation.getBuckets().stream().map(bucket -> new ElasticsearchUtil.AggregationDetail(bucket.getKeyAsString(), bucket.getDocCount(), ((Sum) bucket.getAggregations().get(sumField)).getValue())).collect(Collectors.toList());
        } else {
            return aggregation.getBuckets().stream().map(bucket -> new ElasticsearchUtil.AggregationDetail(bucket.getKeyAsString(), bucket.getDocCount())).collect(Collectors.toList());
        }
    }

    /**
     * 聚合查询明细
     */
    public class AggregationDetail {

        /**
         * 日期
         */
        private String date;

        /**
         * 数量
         */
        private Long count;

        /**
         * 容量
         */
        private Double size;

        public AggregationDetail() {
        }

        public AggregationDetail(String date, Long count) {
            this.date = date;
            this.count = count;
        }

        public AggregationDetail(String date, Long count, Double size) {
            this.date = date;
            this.count = count;
            this.size = size;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Double getSize() {
            return size;
        }

        public void setSize(Double size) {
            this.size = size;
        }
    }

    /**
     * 日期类型枚举
     *
     * @author huangTT(bluehtt @ gmail.com)
     * @version 1.0
     * @date 2021/1/21
     */
    public enum DateFlagEnum {

        /**
         * 日
         */
        DAY(1, "日"),

        /**
         * 月
         */
        MONTH(2, "月"),

        /**
         * 年
         */
        YEAR(3, "年");

        private final Integer code;

        private final String name;

        DateFlagEnum(Integer code, String name) {
            this.code = code;
            this.name = name;
        }

        public Integer getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 获取范围查询条件
     *
     * @param field 查询属性
     * @param begin 开始
     * @param end   结束
     * @return {@link RangeQueryBuilder} 范围查询条件
     */
    public RangeQueryBuilder getRangeQueryBuilder(String field, Object begin, Object end) {
        RangeQueryBuilder rangeQueryBuilder = null;
        if (begin != null) {
            rangeQueryBuilder = QueryBuilders.rangeQuery(field).gte(begin);
        }
        if (end != null) {
            rangeQueryBuilder = (rangeQueryBuilder == null) ? QueryBuilders.rangeQuery(field).lte(end) : rangeQueryBuilder.lte(end);
        }
        return rangeQueryBuilder;
    }

    /**
     * 获取geoShape查询条件。多边形都没有内嵌图形。
     *
     * @param coordinatesList 坐标点图形列表
     * @return {@link GeoShapeQueryBuilder} 地理信息查询条件
     */
    public GeoShapeQueryBuilder getGeoShapeQueryBuilder(List<List<Coordinate>> coordinatesList) throws IOException {
        if (coordinatesList == null || coordinatesList.size() == 0) {
            return null;
        }
        MultiPolygonBuilder shapeBuilder = new MultiPolygonBuilder();
        coordinatesList.stream().map(coordinates -> new PolygonBuilder(new CoordinatesBuilder().coordinates(coordinates))).forEach(shapeBuilder::polygon);

        return QueryBuilders.geoShapeQuery(LOCATION, shapeBuilder).relation(ShapeRelation.INTERSECTS);
    }

    /**
     * filter query设置must条件
     *
     * @param filterQueryBuilder 过滤查询实体
     * @param queryBuilders      查询构建类
     */
    public void filterMustQueryBuilder(BoolQueryBuilder filterQueryBuilder, QueryBuilder... queryBuilders) {
        Arrays.stream(queryBuilders).filter(Objects::nonNull).forEach(queryBuilder -> filterQueryBuilder.must().add(queryBuilder));
    }

    /**
     * filter query设置mustNot条件
     *
     * @param filterQueryBuilder 过滤查询实体
     * @param queryBuilders      查询构建类
     */
    public void filterMustNotQueryBuilder(BoolQueryBuilder filterQueryBuilder, QueryBuilder... queryBuilders) {
        Arrays.stream(queryBuilders).filter(Objects::nonNull).forEach(queryBuilder -> filterQueryBuilder.mustNot().add(queryBuilder));
    }

    /**
     * filter query设置should条件
     *
     * @param filterQueryBuilder 过滤查询实体
     * @param queryBuilders      查询构建类
     */
    public void filterShouldQueryBuilder(BoolQueryBuilder filterQueryBuilder, QueryBuilder... queryBuilders) {
        Arrays.stream(queryBuilders).filter(Objects::nonNull).forEach(queryBuilder -> filterQueryBuilder.should().add(queryBuilder));
        filterQueryBuilder.minimumShouldMatch(1);
    }

    /**
     * 插入数据
     *
     * @param index   索引名称
     * @param id      数据id
     * @param dataMap 数据信息键值对
     * @return boolean
     */
    public boolean add(String index, String id, Map<String, Object> dataMap) throws IOException {
        LOG.info("es开始插入数据，id=[{}]", id);
        IndexRequest indexRequest = new IndexRequest(index).id(id).source(dataMap);
        IndexResponse response = elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);

        return operationValid(response, DocWriteResponse.Result.CREATED);
    }

    /**
     * 插入数据: 数据id已存在更新，没有则新增
     *
     * @param index   索引名称
     * @param id      数据id
     * @param dataMap 数据信息键值对
     * @return boolean
     */
    public boolean addStrict(String index, String id, Map<String, Object> dataMap) throws IOException {
        LOG.info("es开始插入数据，id=[{}]", id);
        return update(index, id, dataMap);
    }

    /**
     * 批量插入数据
     *
     * @param index 索引名称
     * @param data  数据信息
     */
    @Deprecated
    public void addBatch(String index, Map<String, Map<String, Object>> data) {
        LOG.info("es开始批量插入数据");
        data.forEach((id, dataMap) -> {
            try {
                addStrict(index, id, dataMap);
                LOG.info("es插入数据成功，id=[{}]", id);
            } catch (IOException e) {
                LOG.error("es插入数据异常, id=[" + id + "]", e);
            }
        });
    }

    /**
     * 批量索引数据
     *
     * @param indexList 索引的数据列表
     * @return {@code BulkItemResponse[]} 批量操作结果返回
     */
    public BulkItemResponse[] addBatch(List<BulkRequestEntity> indexList) throws IOException {
        LOG.info("es开始批量插入数据：{}", indexList.size());
        return bulk(indexList, null, null);
    }

    /**
     * 批量操作数据
     *
     * @param indexList  索引的数据列表
     * @param updateList 更新的数据列表
     * @param deleteList 删除的数据列表
     * @return {@code BulkItemResponse[]} 批量操作结果返回
     */
    public BulkItemResponse[] bulk(List<BulkRequestEntity> indexList,
                                   List<BulkRequestEntity> updateList,
                                   List<BulkRequestEntity> deleteList) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        // 配置索引参数
        if (indexList != null) {
            indexList.forEach(indexEntity -> {
                IndexRequest indexRequest = new IndexRequest(indexEntity.getIndex()).source(indexEntity.getDataMap());
                if (!StringUtils.isBlank(indexEntity.getId())) {
                    // 不传入id，由es自动生成
                    indexRequest.id(indexEntity.getId());
                }
                bulkRequest.add(indexRequest);
            });
        }
        // 配置更新参数
        if (updateList != null) {
            updateList.forEach(updateEntity -> {
                UpdateRequest updateRequest = new UpdateRequest(updateEntity.getIndex(), updateEntity.getId());
                updateRequest.doc(updateEntity.getDataMap());
                bulkRequest.add(updateRequest);
            });
        }
        // 配置删除参数
        if (deleteList != null) {
            deleteList.forEach(deleteEntity -> bulkRequest.add(new DeleteRequest(deleteEntity.getIndex(), deleteEntity.getId())));
        }

        // 发送请求
        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return bulkResponse.getItems();
    }

    /**
     * 异步插入数据
     *
     * @param index   索引名称
     * @param id      数据id
     * @param dataMap 数据信息键值对
     */
    public void addAsync(String index, String id, Map<String, Object> dataMap, ActionListener<IndexResponse> listener) {
        LOG.info("es开始异步插入数据，id=[{}]", id);
        IndexRequest indexRequest = new IndexRequest(index).id(id).source(dataMap);
        elasticsearchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
    }

    /**
     * 更新数据, 数据不存在则新增
     *
     * @param index   索引名称
     * @param id      数据id
     * @param dataMap 数据信息键值对
     * @return boolean
     */
    public boolean update(String index, String id, Map<String, Object> dataMap) throws IOException {
        LOG.info("es开始更新数据，id=[{}]", id);
        UpdateRequest updateRequest = new UpdateRequest(index, id).doc(dataMap).upsert(dataMap);
        UpdateResponse response = elasticsearchClient.update(updateRequest, RequestOptions.DEFAULT);

        return operationValid(response, DocWriteResponse.Result.UPDATED) || operationValid(response, DocWriteResponse.Result.CREATED);
    }

    /**
     * 异步更新数据, 数据不存在则新增
     *
     * @param index   索引名称
     * @param id      数据id
     * @param dataMap 数据信息键值对
     */
    public void updateAsync(String index, String id, Map<String, Object> dataMap, ActionListener<UpdateResponse> listener) {
        LOG.info("es开始异步更新数据，id=[{}]", id);
        UpdateRequest updateRequest = new UpdateRequest(index, id).doc(dataMap).upsert(dataMap);
        elasticsearchClient.updateAsync(updateRequest, RequestOptions.DEFAULT, listener);
    }

    /**
     * 删除数据
     *
     * @param index 索引名称
     * @param id    数据id
     * @return boolean
     */
    public boolean delete(String index, String id) throws IOException {
        LOG.info("es开始删除数据，id=[{}]", id);
        DeleteRequest deleteRequest = new DeleteRequest(index, id);
        DeleteResponse response = elasticsearchClient.delete(deleteRequest, RequestOptions.DEFAULT);

        return operationValid(response, DocWriteResponse.Result.DELETED);
    }

    /**
     * 根据查询条件删除数据
     *
     * @param queryBuilder 查询条件构建类
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     */
    public List<Map<String, String>> deleteBatch(QueryBuilder queryBuilder) throws IOException {
        LOG.info("es开始批量删除数据");
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(getIndex()).setQuery(queryBuilder);
        BulkByScrollResponse response = elasticsearchClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);

        return response.getBulkFailures().stream().map(failure -> {
            Exception cause = failure.getCause();
            LOG.error("es批量删除数据失败：id=[{" + failure.getId() + "}]", cause);

            Map<String, String> valueMap = new HashMap<>(1);
            valueMap.put(failure.getId(), failure.getCause().getMessage());
            return valueMap;
        }).collect(Collectors.toList());
    }

    /**
     * 查询数据信息，使用id查询。无法使用别名，使用别名查询推荐 {@code this.select(index, id)}
     *
     * @param index 索引名称
     * @param id    数据id
     * @return {@code Map<String, Object>} 查询结果
     */
    public Map<String, Object> selectById(String index, String id) throws IOException {
        GetRequest getRequest = new GetRequest(index, id);
        GetResponse response = elasticsearchClient.get(getRequest, RequestOptions.DEFAULT);

        return response.isSourceEmpty() ? null : response.getSource();
    }

    /**
     * 查询数据信息
     *
     * @param index 索引名称
     * @param id    数据id
     * @return {@code Map<String, Object>} 查询结果
     */
    public Map<String, Object> select(String index, String id) throws IOException {
        List<Map<String, Object>> maps = selectBatch(index, id);
        if (maps == null || maps.isEmpty()) {
            return null;
        }
        return maps.get(0);
    }

    /**
     * 根据多个id查询数据
     *
     * @param index 索引名称
     * @param ids   id数组
     * @return {@code List<Map<String, Object>>} 查询结果列表，结果信息以 {@code Map<String, Object>} 组织
     */
    public List<Map<String, Object>> selectBatch(String index, String... ids) throws IOException {
        LOG.info("根据ids批量查询数据开始，ids={}", Arrays.toString(ids));
        long beginTime = System.currentTimeMillis();
        // 封装查询条件
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.boolQuery().must(QueryBuilders.idsQuery().addIds(ids)));
        SearchRequest searchRequest = new SearchRequest(index).source(new SearchSourceBuilder().query(queryBuilder).from(0).size(ids.length));
        // 数据查询
        SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        long endTime = System.currentTimeMillis();

        LOG.info("根据ids批量查询数据，耗时=[{}]ms", (endTime - beginTime));
        return hits.length == 0 ? null : Stream.of(hits).map(hit -> {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            sourceAsMap.put(ID, hit.getId());
            return sourceAsMap;
        }).collect(Collectors.toList());
    }

    /**
     * 数据是否存在
     *
     * @param index 索引名称
     * @param id    数据id
     * @return boolean
     */
    public boolean isExist(String index, String id) throws IOException {
        GetRequest getRequest = new GetRequest(index, id);
        return elasticsearchClient.exists(getRequest, RequestOptions.DEFAULT);
    }

    /**
     * 操作是否成功
     *
     * @param response 操作返回结果
     * @param result   操作类型
     * @return boolean
     */
    private boolean operationValid(DocWriteResponse response, DocWriteResponse.Result result) {
        return response != null && result.equals(response.getResult());
    }

    /**
     * 获取索引名称
     *
     * @return String
     */
    private String getIndex() {
        return properties.getMetadataIndex();
    }

    /**
     * 日期工具
     *
     * @author huangTT(bluehtt @ gmail.com)
     * @version 1.0
     * @date 2019/5/6
     */
    private static class DateUtil {

        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String MONTH_FORMAT = "yyyy-MM";
        public static final String YEAR_FORMAT = "yyyy";

        public static final String QUERY_BEGIN_TIME = " 00:00:00";
        public static final String QUERY_END_TIME = " 23:59:59";
        public static final String QUERY_BEGIN_MONTH = "-01 00:00:00";
        public static final String QUERY_END_MONTH = "-31 23:59:59";
        public static final String QUERY_BEGIN_YEAR = "-01-01 00:00:00";
        public static final String QUERY_END_YEAR = "-12-31 23:59:59";

        /**
         * 根据给定的日期和格式，返回日期字符串
         *
         * @param date   指定日期
         * @param format 日期格式
         * @return String 格式化后的时间字符串
         */
        private static String getDateString(Date date, String format) {
            date = (date == null) ? Calendar.getInstance().getTime() : date;
            return new SimpleDateFormat(format).format(date);
        }

        /**
         * 获取当前时间间隔指定天的日期
         *
         * @param day 间隔天数
         * @return String 时间字符串yyyy-MM-dd
         */
        public static String getDateByIntervalDay(int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, day);
            return getDateString(calendar.getTime(), DATE_FORMAT);
        }

        /**
         * 获取当前时间间隔指定天的日期
         *
         * @param month 间隔月份
         * @return yyyy-MM
         */
        public static String getDateByIntervalMonth(int month) {
            Calendar calendar = getInstance();
            calendar.add(Calendar.MONTH, month);
            return getDateString(calendar.getTime(), MONTH_FORMAT);
        }

        /**
         * 获取当前时间间隔指定天的日期
         *
         * @param year 间隔年数
         * @return String
         */
        public static String getDateByIntervalYear(int year) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, year);
            return getDateString(calendar.getTime(), YEAR_FORMAT);
        }

        /**
         * 根据日期拼接查询开始时间
         *
         * @param beginDate yyyy-MM-dd
         * @return String 时间字符串yyyy-MM-dd 00:00:00
         */
        public static String getQueryBeginTimeByDate(String beginDate) {
            return (beginDate == null) ? null : (beginDate + QUERY_BEGIN_TIME);
        }

        /**
         * 根据日期拼接查询结束时间
         *
         * @param endDate yyyy-MM-dd
         * @return string 时间字符串yyyy-MM-dd 23:59:59
         */
        public static String getQueryEndTimeByDate(String endDate) {
            return (endDate == null) ? null : (endDate + QUERY_END_TIME);
        }

        /**
         * 根据日期拼接查询开始时间
         *
         * @param beginDate 日期字符串，yyyy-MM
         * @return String 时间字符串yyyy-MM-01 00:00:00
         */
        public static String getQueryBeginTimeByMonth(String beginDate) {
            return (beginDate == null) ? null : (beginDate + QUERY_BEGIN_MONTH);
        }

        /**
         * 根据日期拼接查询结束时间
         *
         * @param beginDate 日期字符串，yyyy-MM
         * @return String 时间字符串yyyy-MM-31 23:59:59
         */
        public static String getQueryEndTimeByMonth(String beginDate) {
            return (beginDate == null) ? null : (beginDate + QUERY_END_MONTH);
        }

        /**
         * 根据日期拼接查询开始时间
         *
         * @param beginDate 日期字符串，yyyy
         * @return String 时间字符串yyyy-01-01 00:00:00
         */
        public static String getQueryBeginTimeByYear(String beginDate) {
            return (beginDate == null) ? null : (beginDate + QUERY_BEGIN_YEAR);
        }

        /**
         * 根据日期拼接查询结束时间
         *
         * @param beginDate 日期字符串，yyyy
         * @return String 时间字符串yyyy-12-31 23:59:59
         */
        public static String getQueryEndTimeByYear(String beginDate) {
            return (beginDate == null) ? null : (beginDate + QUERY_END_YEAR);
        }
    }

}
