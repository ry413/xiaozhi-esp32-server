package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "激活码作废参数")
public class ActivationCodeVoidDTO {

    @Schema(description = "作废原因")
    @NotBlank(message = "作废原因不能为空")
    private String reason;
}
