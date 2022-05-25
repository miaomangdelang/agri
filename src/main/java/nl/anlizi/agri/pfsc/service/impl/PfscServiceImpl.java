package nl.anlizi.agri.pfsc.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import nl.anlizi.agri.pfsc.constant.ApplicationConstant;
import nl.anlizi.agri.pfsc.service.PfscService;
import nl.anlizi.agri.pfsc.transfer.input.BaseInput;
import nl.anlizi.agri.pfsc.transfer.output.EveryoneRunStatus;
import nl.anlizi.agri.pfsc.transfer.output.PriceQuotationPageListOutput;
import nl.anlizi.agri.pfsc.util.ElasticsearchUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import org.zheos.elasticsearch.config.ElasticsearchProperties;
import org.zheos.elasticsearch.model.entity.BulkRequestEntity;
import org.zheos.http.util.HttpUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 获取全国农业信息网服务类
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/18 21:10
 */
@Service
@Slf4j
public class PfscServiceImpl implements PfscService {

    @Resource
    private ElasticsearchProperties elasticsearchConfig;

    @Resource
    private ElasticsearchUtil elasticsearchUtil;

    /**
     * 爬虫获取全国农业信息网
     */
    @Override
    public void reptilePfsc() {
        String dateFormat = DateFormatUtils.format(new Date(), ApplicationConstant.DATE_FORMAT);
        // 校验今天是否成功
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery().filter(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(ApplicationConstant.UPDATE_DATE, dateFormat))
                .must(QueryBuilders.termQuery(ApplicationConstant.IS_SUCCESS, true))
                .must(QueryBuilders.termQuery(ApplicationConstant.INDEX_NAME, elasticsearchConfig.getMetadataIndex())));
        SearchSourceBuilder query = new SearchSourceBuilder().query(filterQuery);
        List<Map<String, Object>> queryCount;
        try {
            queryCount = elasticsearchUtil.search(ApplicationConstant.EVERYONE_RUN_STATUS, query);
        } catch (IOException e) {
            log.error("查询状态出错：{}", dateFormat, e);
            return;
        }
        if (queryCount.size() > 0) {
            log.warn("今天已尝试过：{}", dateFormat);
            return;
        }
        // 初始化第一页
        BaseInput returnBaseInput = new BaseInput(1 , ApplicationConstant.DEFAULT_PAGE_SIZE, false, false, 0);
        while (!returnBaseInput.isGetEnd()) {
            log.warn("开始爬虫page：{}", returnBaseInput.getPageNum());
            reptilePageOne(returnBaseInput);
            log.warn("是否结束page：{}, is:{}, ", returnBaseInput.getPageNum(), returnBaseInput.isGetEnd());
            returnBaseInput.setPageNum(returnBaseInput.getPageNum() + 1);
        }
        String timeFormat = DateFormatUtils.format(new Date(), ApplicationConstant.DATE_TIME_FORMAT);
        // 统计今日总数
        int dayCount = returnBaseInput.getPageSize() * (returnBaseInput.getPageNum() - 1) + returnBaseInput.getCurrentCount();
        // 是否全部成功
        boolean isSuccess = returnBaseInput.isGetEnd() && returnBaseInput.isGetPageSuccess();
        EveryoneRunStatus everyoneRunStatus = new EveryoneRunStatus(elasticsearchConfig.getMetadataIndex(), isSuccess, timeFormat, dateFormat, dayCount);
        try {
            elasticsearchUtil.add(ApplicationConstant.EVERYONE_RUN_STATUS, String.valueOf(System.currentTimeMillis()), JSON.parseObject(JSON.toJSONString(everyoneRunStatus), Map.class));
        } catch (IOException e) {
            log.error("添加状态失败：{}", everyoneRunStatus, e);
        }

    }

    /**
     * 获取一页信息
     *
     * @param input 分页信息 pageNum、pageSize
     */
    private void reptilePageOne(BaseInput input) {
        try {
            String timeFormat = DateFormatUtils.format(new Date(), ApplicationConstant.DATE_FORMAT);
            PriceQuotationPageListOutput outputPage = HttpUtil.post(ApplicationConstant.PFSC_URL, input, PriceQuotationPageListOutput.class, headerMap());
            int size = outputPage.getContent().getList().size();
            log.warn("获得数据量：{}", size);
            List<BulkRequestEntity> insertList = outputPage.getContent().getList().stream().map(entity -> {
                Map<String, Object> paramMap = JSON.parseObject(JSON.toJSONString(entity), Map.class);
                paramMap.put(ApplicationConstant.CREATE_TIME, new Date().getTime());
                paramMap.put(ApplicationConstant.CREATE_DATE, timeFormat);
                return new BulkRequestEntity(elasticsearchConfig.getMetadataIndex(), entity.getId(), paramMap);
            }).collect(Collectors.toList());
            try {
                elasticsearchUtil.addBatch(insertList);
            } catch (Exception e) {
                input.setGetEnd(true);
                log.error("elastic插入数据异常:{}", input, e);
                return;
            }
            input.setGetEnd(outputPage.getContent().getIsLastPage());
            input.setGetPageSuccess(true);
            input.setCurrentCount(size);
            return;
        } catch (IOException e) {
            log.error("农业信息网爬取失败IOException:{}", input, e);
        } catch (URISyntaxException e) {
            log.error("农业信息网爬取失败URISyntaxException:{}", input, e);
        }
        input.setGetPageSuccess(false);
    }

    private Map<String, String> headerMap() {
        Map<String, String> map = new HashMap<>(16);
        map.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        map.put("Accept-Language", "zh-CN,zh;q=0.8");
        map.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        return map;
    }

}
