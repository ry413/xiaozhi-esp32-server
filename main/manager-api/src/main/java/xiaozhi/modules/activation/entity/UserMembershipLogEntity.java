package xiaozhi.modules.activation.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user_membership_log")
@Schema(description = "用户月卡权益流水")
public class UserMembershipLogEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "权益ID")
    private Long membershipId;

    @Schema(description = "变更类型")
    private String changeType;

    @Schema(description = "变更前开始时间")
    private Date startAtBefore;

    @Schema(description = "变更前结束时间")
    private Date endAtBefore;

    @Schema(description = "变更后开始时间")
    private Date startAtAfter;

    @Schema(description = "变更后结束时间")
    private Date endAtAfter;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
