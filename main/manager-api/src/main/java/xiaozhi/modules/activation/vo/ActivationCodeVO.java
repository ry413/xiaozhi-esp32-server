package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "激活码明细")
public class ActivationCodeVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "卡类型")
    private String cardType;

    @Schema(description = "面值")
    private Integer faceValue;

    @Schema(description = "激活码")
    private String code;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "可用开始时间")
    private Date validFrom;

    @Schema(description = "可用结束时间")
    private Date validUntil;

    @Schema(description = "使用者用户ID")
    private Long usedUserId;

    @Schema(description = "使用时间")
    private Date usedAt;

    @Schema(description = "作废时间")
    private Date voidAt;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "创建时间")
    private Date createDate;
}
