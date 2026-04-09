package xiaozhi.modules.liveplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员直播方案分页参数")
public class LivePlanAdminPageDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    private String limit;
}
