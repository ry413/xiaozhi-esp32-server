package xiaozhi.modules.agent.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体大模型调用记录表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "ai_agent_llm_report")
public class AgentLlmReportEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "mac_address")
    private String macAddress;

    @TableField(value = "client_id")
    private String clientId;

    @TableField(value = "client_ip")
    private String clientIp;

    @TableField(value = "agent_id")
    private String agentId;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "session_id")
    private String sessionId;

    @TableField(value = "llm_input")
    private String llmInput;

    @TableField(value = "llm_output")
    private String llmOutput;

    @TableField(value = "created_at")
    private Date createdAt;

    @TableField(value = "updated_at")
    private Date updatedAt;
}
