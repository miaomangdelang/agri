package nl.anlizi.agri.pfsc.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import nl.anlizi.agri.pfsc.constant.ApplicationConstant;
import nl.anlizi.agri.pfsc.service.PfscService;
import nl.anlizi.agri.pfsc.transfer.input.BaseInput;
import nl.anlizi.agri.pfsc.transfer.output.PriceQuotationPageListOutput;
import nl.anlizi.agri.pfsc.util.ElasticsearchUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
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
        boolean isGetEnd = false;
        // 初始化第一页
        int pageNum = 1;
        while (!isGetEnd) {
            log.warn("开始爬虫page：{}", pageNum);
            isGetEnd = reptilePageOne(new BaseInput(pageNum, ApplicationConstant.DEFAULT_PAGE_SIZE));
            log.warn("是否结束page：{}, is:{}", pageNum, isGetEnd);
            pageNum++;
        }
    }

    /**
     * 获取一页信息
     *
     * @param input 分页信息 pageNum、pageSize
     * @return 是否获取完毕
     */
    private boolean reptilePageOne(BaseInput input) {
        try {
            String timeFormat = DateFormatUtils.format(new Date(), ApplicationConstant.DATETIME_FORMAT);
            PriceQuotationPageListOutput outputPage = HttpUtil.post(ApplicationConstant.PFSC_URL, input, PriceQuotationPageListOutput.class, headerMap());
            log.warn("获得数据量：{}", outputPage.getContent().getList().size());
            List<BulkRequestEntity> insertList = outputPage.getContent().getList().stream().map(entity -> {
                Map<String, Object> paramMap = JSON.parseObject(JSON.toJSONString(entity), Map.class);
                paramMap.put(ApplicationConstant.CREATE_TIME, new Date().getTime());
                paramMap.put(ApplicationConstant.CREATE_DATE, timeFormat);
                return new BulkRequestEntity(elasticsearchConfig.getMetadataIndex(), entity.getId(), paramMap);
            }).collect(Collectors.toList());
            try {
                elasticsearchUtil.addBatch(insertList);
            } catch (Exception e) {
                log.error("elastic插入数据异常", e);
                return true;
            }
            return outputPage.getContent().getIsLastPage();
        } catch (IOException e) {
            log.error("农业信息网爬取失败IOException:{}", input, e);
        } catch (URISyntaxException e) {
            log.error("农业信息网爬取失败URISyntaxException:{}", input, e);
        }
        return true;
    }

    private Map<String, String> headerMap() {
        Map<String, String> map = new HashMap<>(16);
        map.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        map.put("Accept-Language", "zh-CN,zh;q=0.8");
        map.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        return map;
    }

}
