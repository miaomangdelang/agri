package nl.anlizi.agri.pfsc.service.impl;

import lombok.extern.slf4j.Slf4j;
import nl.anlizi.agri.pfsc.service.ViewService;
import nl.anlizi.agri.pfsc.util.ElasticsearchUtil;
import org.springframework.stereotype.Service;
import org.zheos.elasticsearch.config.ElasticsearchProperties;

import javax.annotation.Resource;

/**
 * 查询服务
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/25 16:19
 */
@Service
@Slf4j
public class ViewServiceImpl implements ViewService {

    @Resource
    private ElasticsearchProperties elasticsearchConfig;

    @Resource
    private ElasticsearchUtil elasticsearchUtil;




}
