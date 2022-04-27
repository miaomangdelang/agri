package nl.anlizi.agri.pfsc.transfer.output;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 每次执行的状态
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/24 21:48
 */
@Data
@AllArgsConstructor
public class EveryoneRunStatus implements Serializable {

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 是否成功
     */
    private Boolean isSuccess;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 更新日期
     */
    private String updateDate;

    /**
     * 今日数量
     */
    private Integer dayCount;

}
