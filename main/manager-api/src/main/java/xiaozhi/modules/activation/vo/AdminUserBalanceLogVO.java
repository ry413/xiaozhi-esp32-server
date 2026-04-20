package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-用户点卡余额流水")
public class AdminUserBalanceLogVO {

    private Long id;
    private Long userId;
    private String username;
    private Long accountId;
    private String changeType;
    private Integer deltaMinutes;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String sourceType;
    private Long sourceId;
    private String remark;
    private Date createdAt;
}
