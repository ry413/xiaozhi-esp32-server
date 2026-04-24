package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员-用户权益摘要")
public class AdminUserBenefitVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户状态")
    private Integer userStatus;

    @Schema(description = "剩余秒数")
    private Integer balanceSeconds;

    @Schema(description = "累计充值秒数")
    private Integer totalRechargedSeconds;

    @Schema(description = "累计消费秒数")
    private Integer totalConsumedSeconds;

    @Schema(description = "余额账户状态")
    private Integer accountStatus;

    @Schema(description = "是否有生效中的月卡")
    private Boolean membershipActive;

    @Schema(description = "当前月卡状态")
    private Integer membershipStatus;

    @Schema(description = "月卡开始时间")
    private Date membershipStartAt;

    @Schema(description = "月卡结束时间")
    private Date membershipEndAt;

    @Schema(description = "月卡每日限额秒数")
    private Integer membershipDailyLimitSeconds;

    @Schema(description = "月卡今日已消费秒数")
    private Integer membershipDailyConsumedSeconds;

    @Schema(description = "月卡今日剩余额度秒数")
    private Integer membershipDailyRemainingSeconds;
}
