package nl.anlizi.agri.pfsc.constant;

/**
 * 系统常量类
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/18 21:20
 */
public class ApplicationConstant {

    /**
     * 农业信息网地址
     */
    public static final String PFSC_URL = "http://pfsc.agri.cn/api/priceQuotationController/pageList?key=&order=";

    /**
     * 默认每页数据量
     */
    public static final int DEFAULT_PAGE_SIZE = 1000;

    /**
     * 创建时间
     */
    public static final String CREATE_TIME = "createTime";

    /**
     * 创建时间格式化
     */
    public static final String CREATE_DATE = "createDate";

    /**
     * 格式化时间 date
     */
    public static String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 状态索引
     */
    public static final String EVERYONE_RUN_STATUS = "everyone-run-status";

    /**
     * 格式化时间 datetime
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 状态更新时间字段名称
     */
    public static final String UPDATE_DATE = "updateDate";

    /**
     * 状态是否成功字段名称
     */
    public static final String IS_SUCCESS = "isSuccess";

    /**
     * 状态索引名称字段
     */
    public static final String INDEX_NAME = "indexName";

}
