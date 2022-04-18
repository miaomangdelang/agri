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

    private Boolean pageable;

    private Integer pageNum;

    private Integer pageSize;

    private String id;

    private String updateDate;

    private String updaterId;

    private String updaterName;

    private String remark;

    private String marketId;

    private String marketCode;

    private String marketName;

    private String szsmMarketName;

    private Integer provinceCode;

    private String provinceName;

    private Integer areaCode;

    private String areaName;

    private String reportId;

    private String meteringUnit;

    private Double minimumPrice;

    private Double middlePrice;

    private Double highestPrice;

    private Double finalPrice;

    private String tradingVolume;

    private String totalPrice;

    private String producePlace;

    private String salePlace;

    private String fluctuate;

    private String tradingDate;

    private String approvalDate;

    private String approvalState;

    private String exceptionType;

    private String varietyId;

    private String varietyCode;

    private String varietyName;

    private Date inStorageTime;

    private String reportTime;

    private String finalState;

    private String marketType;

    private String marketCategory;

    private String unitType;

    private String isKeyCollect;

    private String varietyTypeId;

    private String varietyTypeCode;

    private String varietyTypeName;

    private String farmUserId;

    private String farmUserName;

    private String avgPrice;

    private String cityAvgPrice;

    private String countryAvgPrice;

    private String counts;

}
