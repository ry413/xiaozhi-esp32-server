package xiaozhi.modules.agent.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-大模型调用记录")
public class AgentLlmReportVO {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID/MAC地址")
    private String macAddress;

    @Schema(description = "客户端ID")
    private String clientId;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "智能体ID")
    private String agentId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "大模型输入")
    private String llmInput;

    @Schema(description = "大模型输出")
    private String llmOutput;

    @Schema(description = "创建时间/上报时间")
    private Date createdAt;
}
