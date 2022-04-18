package nl.anlizi.agri.pfsc.transfer.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础分页入参
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/17 20:20
 */
@Data
@AllArgsConstructor
public class BaseInput implements Serializable {

    /**
     * 当前页
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;
}
