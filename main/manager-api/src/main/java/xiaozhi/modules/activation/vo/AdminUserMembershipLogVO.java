package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-用户月卡流水")
public class AdminUserMembershipLogVO {

    private Long id;
    private Long userId;
    private String username;
    private Long membershipId;
    private String changeType;
    private Date startAtBefore;
    private Date endAtBefore;
    private Date startAtAfter;
    private Date endAtAfter;
    private String sourceType;
    private Long sourceId;
    private String remark;
    private Date createdAt;
}
