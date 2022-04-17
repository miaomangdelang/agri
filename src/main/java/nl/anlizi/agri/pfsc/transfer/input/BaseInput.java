package nl.anlizi.agri.pfsc.transfer.input;

import java.io.Serializable;

/**
 * 基础分页入参
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/17 20:20
 */
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
