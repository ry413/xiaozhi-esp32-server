package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "点卡余额消费参数")
public class UserBalanceConsumeDTO {

    @Schema(description = "消费秒数")
    @NotNull(message = "seconds不能为空")
    @Min(value = 1, message = "seconds必须大于0")
    private Integer seconds;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;
}
