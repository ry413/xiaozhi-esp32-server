package xiaozhi.modules.activation.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户权益摘要")
public class UserBenefitVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "剩余秒数")
    private Integer balanceSeconds;

    @Schema(description = "是否有生效中的月卡")
    private Boolean membershipActive;

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

    @Schema(description = "本次扣费来源: membership_daily_quota/balance")
    private String chargedFrom;
}
