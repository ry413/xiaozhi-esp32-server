package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-用户月卡权益")
public class AdminUserMembershipVO {

    private Long id;
    private Long userId;
    private String username;
    private String membershipType;
    private Date startAt;
    private Date endAt;
    private Integer status;
    private String sourceType;
    private Long sourceId;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
}
