package nl.anlizi.agri.pfsc.transfer.output;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 产品价格详情
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/17 20:46
 */
@Data
public class PriceQuotationDetailsOutput implements Serializable {

    /**
     * 是否可分页的？
     */
    private Boolean pageable;

    /**
     * 当前页数
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 数据id
     */
    private String id;

    /**
     * 更新时间
     */
    private String updateDate;

    /**
     * 更新者Id
     */
    private String updaterId;

    /**
     * 更新者名称
     */
    private String updaterName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 市场Id
     */
    private String marketId;

    /**
     * 市场Code
     */
    private String marketCode;

    /**
     * 市场名称
     */
    private String marketName;

    /**
     * szsm市场名称
     */
    private String szsmMarketName;

    /**
     * 城市code
     */
    private Integer provinceCode;

    /**
     * 城市名称
     */
    private String provinceName;

    /**
     * 区域(区级)Code
     */
    private Integer areaCode;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 报告Id
     */
    private String reportId;

    /**
     * 计量单元
     */
    private String meteringUnit;

    /**
     * 最低价格
     */
    private Double minimumPrice;

    /**
     * 中间价
     */
    private Double middlePrice;

    /**
     * 最高价格
     */
    private Double highestPrice;

    /**
     * 最终价格
     */
    private Double finalPrice;

    /**
     * 交易量
     */
    private String tradingVolume;

    /**
     * 总价
     */
    private String totalPrice;

    /**
     * 产地
     */
    private String producePlace;

    /**
     * 销售地点
     */
    private String salePlace;

    /**
     * 波动
     */
    private String fluctuate;

    /**
     * 交易日
     */
    private String tradingDate;

    /**
     * 批准日期
     */
    private String approvalDate;

    /**
     * 批准国
     */
    private String approvalState;

    /**
     * 异常类型
     */
    private String exceptionType;

    /**
     * 品种编号
     */
    private String varietyId;

    /**
     * 品种代号
     */
    private String varietyCode;

    /**
     * 品种名称
     */
    private String varietyName;

    /**
     * 存储时间
     */
    private Date inStorageTime;

    /**
     * 报告时间
     */
    private String reportTime;

    /**
     * 最终状态
     */
    private String finalState;

    /**
     * 市场类型
     */
    private String marketType;

    /**
     * 市场类别
     */
    private String marketCategory;

    /**
     * 单位类型
     */
    private String unitType;

    /**
     * 是关键收集
     */
    private String isKeyCollect;

    /**
     * 品种类型 ID
     */
    private String varietyTypeId;

    /**
     * 品种型号代码
     */
    private String varietyTypeCode;

    /**
     * 品种类型名称
     */
    private String varietyTypeName;

    /**
     * 农场用户Id
     */
    private String farmUserId;

    /**
     * 农场用户名称
     */
    private String farmUserName;

    /**
     * 平均价格
     */
    private String avgPrice;

    /**
     * 城市平均价格
     */
    private String cityAvgPrice;

    /**
     * 国家平均价格
     */
    private String countryAvgPrice;

    /**
     * 计数
     */
    private String counts;

}
