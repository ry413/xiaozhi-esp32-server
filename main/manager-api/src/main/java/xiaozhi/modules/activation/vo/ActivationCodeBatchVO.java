package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "激活码批次")
public class ActivationCodeBatchVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "批次名称")
    private String batchName;

    @Schema(description = "卡类型")
    private String cardType;

    @Schema(description = "面值")
    private Integer faceValue;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "可用开始时间")
    private Date validFrom;

    @Schema(description = "可用结束时间")
    private Date validUntil;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "已生成激活码数量")
    private Long generatedCount;

    @Schema(description = "创建时间")
    private Date createDate;
}
