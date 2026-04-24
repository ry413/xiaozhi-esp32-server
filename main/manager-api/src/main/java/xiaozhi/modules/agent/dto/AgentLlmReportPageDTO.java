package xiaozhi.modules.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员-大模型调用记录分页参数")
public class AgentLlmReportPageDTO {
    @Schema(description = "页码")
    @NotBlank(message = "page不能为空")
    private String page;

    @Schema(description = "每页条数")
    @NotBlank(message = "limit不能为空")
    private String limit;

    @Schema(description = "关键词: 用户名/用户ID/设备ID/客户端ID/会话ID/输入/输出")
    private String keyword;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "设备ID/MAC地址")
    private String macAddress;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
