package nl.anlizi.agri.pfsc.schedule;

import lombok.extern.slf4j.Slf4j;
import nl.anlizi.agri.pfsc.service.PfscService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时轮询
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/20 21:03
 */
@Slf4j
@Component
public class PfscSchedule {

    @Resource
    private PfscService pfscService;

    /**
     * 爬虫获取全国农业信息网
     */
    @Scheduled(cron = "${cron.pfsc}")
    public void reptilePfsc() {
        pfscService.reptilePfsc();
    }
}
