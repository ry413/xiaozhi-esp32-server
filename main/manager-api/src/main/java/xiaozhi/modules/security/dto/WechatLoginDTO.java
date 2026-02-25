package xiaozhi.modules.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信登录表单
 */
@Data
@Schema(description = "微信登录表单")
public class WechatLoginDTO {
    @Schema(description = "wx.login()返回的code")
    private String code;
}
