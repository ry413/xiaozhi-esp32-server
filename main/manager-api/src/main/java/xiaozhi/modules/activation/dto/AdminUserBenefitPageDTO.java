package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员-用户权益分页参数")
public class AdminUserBenefitPageDTO {

    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    private String limit;

    @Schema(description = "用户关键词(用户ID或用户名)")
    private String keyword;
}
