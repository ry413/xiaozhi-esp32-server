package xiaozhi.modules.liveplan.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "直播方案更新参数")
public class LivePlanUpdateDTO {

    @Schema(description = "方案名称")
    @NotBlank(message = "planName不能为空")
    private String planName;

    @Schema(description = "直播平台")
    @NotBlank(message = "platform不能为空")
    private String platform;

    @Schema(description = "直播间ID")
    @NotBlank(message = "roomId不能为空")
    private String roomId;

    @Schema(description = "方案配置JSON")
    private Map<String, Object> configJson;

    @Schema(description = "状态: 0=空闲, 1=使用中")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
