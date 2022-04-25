package nl.anlizi.agri.pfsc;

import nl.anlizi.agri.pfsc.service.PfscService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

/**
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/18 22:19
 */
@SpringBootTest
//@ActiveProfiles("develop")
public class PfscServiceTest {

    @Resource
    private PfscService pfscService;

    @Test
    public void reptilePfsc() {
        pfscService.reptilePfsc();
    }

}
