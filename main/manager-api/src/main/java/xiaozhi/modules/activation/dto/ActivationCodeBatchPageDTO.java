package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "激活码批次分页参数")
public class ActivationCodeBatchPageDTO {

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "批次名称")
    private String batchName;

    @Schema(description = "卡类型: point/month")
    private String cardType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    @Min(value = 1, message = "page必须大于0")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    @Min(value = 1, message = "limit必须大于0")
    private String limit;
}
