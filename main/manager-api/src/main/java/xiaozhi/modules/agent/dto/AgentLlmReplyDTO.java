package xiaozhi.modules.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "LLM文本回复请求")
public class AgentLlmReplyDTO {

    @Schema(description = "用户输入文本")
    @NotBlank(message = "prompt不能为空")
    private String prompt;
}
