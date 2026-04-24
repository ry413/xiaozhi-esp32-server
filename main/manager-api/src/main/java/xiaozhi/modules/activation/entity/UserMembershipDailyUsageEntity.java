package xiaozhi.modules.activation.entity;

import java.time.LocalDate;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user_membership_daily_usage")
@Schema(description = "用户月卡每日用量")
public class UserMembershipDailyUsageEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "月卡权益ID")
    private Long membershipId;

    @Schema(description = "用量日期")
    private LocalDate usageDate;

    @Schema(description = "每日限额秒数")
    private Integer dailyLimitSeconds;

    @Schema(description = "已消费秒数")
    private Integer consumedSeconds;

    @Schema(description = "状态:0禁用,1正常")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updatedAt;
}
