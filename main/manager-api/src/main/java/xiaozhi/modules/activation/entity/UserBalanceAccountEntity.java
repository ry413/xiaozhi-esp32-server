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
@TableName("user_balance_account")
@Schema(description = "用户点卡余额账户")
public class UserBalanceAccountEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "当前剩余秒数")
    private Integer balanceMinutes;

    @Schema(description = "累计充值秒数")
    private Integer totalRechargedMinutes;

    @Schema(description = "累计消费秒数")
    private Integer totalConsumedMinutes;

    @Schema(description = "状态:0禁用,1正常")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updatedAt;
}
