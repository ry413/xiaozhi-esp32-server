package xiaozhi.modules.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小智服务大模型调用上报请求
 */
@Data
@Schema(description = "小智服务大模型调用上报请求")
public class AgentLlmReportDTO {
    @Schema(description = "MAC地址/设备ID", example = "00:11:22:33:44:55")
    @NotBlank
    private String macAddress;

    @Schema(description = "客户端ID", example = "web_test_client")
    private String clientId;

    @Schema(description = "客户端IP", example = "192.168.1.10")
    private String clientIp;

    @Schema(description = "会话ID", example = "79578c31-f1fb-426a-900e-1e934215f05a")
    @NotBlank
    private String sessionId;

    @Schema(description = "大模型输入")
    @NotBlank
    private String llmInput;

    @Schema(description = "大模型输出")
    @NotBlank
    private String llmOutput;

    @Schema(description = "上报时间，毫秒时间戳，空时默认使用当前时间", example = "1745657732000")
    private Long reportTime;
}
