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
}
