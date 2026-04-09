package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "权益流水分页参数")
public class UserBenefitLogPageDTO {

    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    @Min(value = 1, message = "page必须大于0")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    @Min(value = 1, message = "limit必须大于0")
    private String limit;
}
