package nl.anlizi.agri.pfsc.transfer.output;

import lombok.Data;

/**
 * 全国农业信息网列表页
 *
 * @author Join.Yao (pathinfuture@163.com)
 * @date 2022/04/17 20:25
 */
@Data
public class PriceQuotationPageListOutput {

    private Integer code;

    private String message;

    private PriceQuotationPageOutput content;

}
