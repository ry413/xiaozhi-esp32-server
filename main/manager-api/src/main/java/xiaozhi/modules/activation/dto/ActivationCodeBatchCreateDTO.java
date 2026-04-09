package xiaozhi.modules.activation.dto;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "激活码批次创建参数")
public class ActivationCodeBatchCreateDTO {

    @Schema(description = "批次号")
    @NotBlank(message = "batchNo不能为空")
    private String batchNo;

    @Schema(description = "批次名称")
    @NotBlank(message = "batchName不能为空")
    private String batchName;

    @Schema(description = "卡类型: point/month")
    @NotBlank(message = "cardType不能为空")
    private String cardType;

    @Schema(description = "面值: point秒数, month天数")
    @NotNull(message = "faceValue不能为空")
    @Min(value = 1, message = "faceValue必须大于0")
    private Integer faceValue;

    @Schema(description = "状态: 0禁用, 1启用")
    private Integer status = 1;

    @Schema(description = "可用开始时间")
    private Date validFrom;

    @Schema(description = "可用结束时间")
    private Date validUntil;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "本次生成激活码数量")
    @NotNull(message = "generateCount不能为空")
    @Min(value = 1, message = "generateCount必须大于0")
    private Integer generateCount;
}
