package xiaozhi.modules.sys.service;

import io.swagger.v3.oas.annotations.media.Schema;
import xiaozhi.common.page.TokenDTO;
import xiaozhi.common.utils.Result;

public interface WechatAuthService {

    @Schema(description = "微信小程序登录或自动注册")
    public Result<TokenDTO> loginOrRegister(String code);
}
