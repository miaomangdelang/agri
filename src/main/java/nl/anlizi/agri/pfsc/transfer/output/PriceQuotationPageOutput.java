package nl.anlizi.agri.pfsc.transfer.output;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 全国农业信息网列表页数据
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/17 20:27
 */
@Data
public class PriceQuotationPageOutput implements Serializable {

    /**
     * 条目总数
     */
    private Long total;

    /**
     * 条目列表
     */
    private List<PriceQuotationDetailsOutput> list;

    /**
     * 实际当前页
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 实际数量
     */
    private Integer size;

    /**
     * 开始行数
     */
    private Long startRow;

    /**
     * 结束行数
     */
    private Long endRow;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 上一页
     */
    private Integer prePage;

    /**
     * 下一页
     */
    private Integer nextPage;

    /**
     * 是第一页
     */
    private Boolean isFirstPage;

    /**
     * 是最后一页
     */
    private Boolean isLastPage;

    /**
     * 有上一页
     */
    private Boolean hasPreviousPage;

    /**
     * 有下一页
     */
    private Boolean hasNextPage;

    /**
     * 导航页数量
     */
    private Integer navigatePages;

    /**
     * 导航页页码列表
     */
    private List<Integer> navigatepageNums;

    /**
     * 导航页码首页码
     */
    private Integer navigateFirstPage;

    /**
     * 导航页码最后一夜
     */
    private Integer navigateLastPage;

    /**
     * 首页码
     */
    private Integer firstPage;

    /**
     * 最后页码
     */
    private Integer lastPage;

}
