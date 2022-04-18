package nl.anlizi.agri.pfsc.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import nl.anlizi.agri.pfsc.constant.ApplicationConstant;
import nl.anlizi.agri.pfsc.service.PfscService;
import nl.anlizi.agri.pfsc.transfer.input.BaseInput;
import nl.anlizi.agri.pfsc.transfer.output.PriceQuotationPageOutput;
import org.springframework.stereotype.Service;
import org.zheos.elasticsearch.config.ElasticsearchConfig;
import org.zheos.elasticsearch.util.ElasticsearchUtil;
import org.zheos.http.util.HttpUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;

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
    private ElasticsearchConfig elasticsearchConfig;

    /**
     * 爬虫获取全国农业信息网
     */
    @Override
    public void reptilePfsc() {
        boolean isGetEnd = false;
        // 初始化第一页
        int pageNum = 1;
        while (!isGetEnd) {
            log.info("开始爬虫page：{}", pageNum);
            isGetEnd = reptilePageOne(new BaseInput(pageNum, ApplicationConstant.DEFAULT_PAGE_SIZE));
            log.info("是否结束page：{}, is:{}", pageNum, isGetEnd);
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
            PriceQuotationPageOutput outputPage = HttpUtil.postByForm(ApplicationConstant.PFSC_URL, input, PriceQuotationPageOutput.class);
            log.info("获得数据量：{}", outputPage.getList().size());
            outputPage.getList().forEach(output -> {
                Map<String, Object> paramMap = JSON.parseObject(JSON.toJSONString(output), Map.class);
                paramMap.put(ApplicationConstant.CREATE_TIME, new Date());
                try {
                    ElasticsearchUtil.addStrict(elasticsearchConfig.getMetadataIndex(), output.getId(), paramMap);
                } catch (Exception e) {
                    log.error("elastic插入数据异常", e);
                }
            });
            return outputPage.getIsLastPage();
        } catch (IOException e) {
            log.error("农业信息网爬取失败IOException:{}", input, e);
        } catch (URISyntaxException e) {
            log.error("农业信息网爬取失败URISyntaxException:{}", input, e);
        }
        return true;
    }

}
