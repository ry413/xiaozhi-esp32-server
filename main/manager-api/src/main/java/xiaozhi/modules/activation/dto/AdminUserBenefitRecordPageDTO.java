package xiaozhi.modules.activation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员-用户权益记录分页参数")
public class AdminUserBenefitRecordPageDTO {

    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    private String limit;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户关键词(用户ID或用户名)")
    private String keyword;

    @Schema(description = "变更类型")
    private String changeType;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "状态")
    private Integer status;
}
