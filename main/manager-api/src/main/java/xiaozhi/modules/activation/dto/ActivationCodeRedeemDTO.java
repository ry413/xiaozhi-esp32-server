package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "激活码兑换参数")
public class ActivationCodeRedeemDTO {

    @Schema(description = "激活码")
    @NotBlank(message = "code不能为空")
    private String code;
}
