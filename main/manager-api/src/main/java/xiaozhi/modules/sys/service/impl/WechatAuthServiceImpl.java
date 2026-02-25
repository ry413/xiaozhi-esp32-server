package xiaozhi.modules.sys.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.TokenDTO;
import xiaozhi.modules.security.service.SysUserTokenService;
import xiaozhi.modules.sys.dto.SysUserDTO;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.SysUserService;
import xiaozhi.modules.sys.service.SysUserWechatService;
import xiaozhi.modules.sys.service.WechatAuthService;
import xiaozhi.modules.sys.utils.WechatMiniClient;
import xiaozhi.modules.sys.utils.WechatSession;
import xiaozhi.common.utils.Result;

@Slf4j
@Service
public class WechatAuthServiceImpl implements WechatAuthService {
    private final WechatMiniClient wechatMiniClient;
    private final SysUserWechatService sysUserWechatService;
    private final SysUserService sysUserService;
    private final SysUserTokenService sysUserTokenService;

    public WechatAuthServiceImpl(WechatMiniClient wechatMiniClient,
            SysUserWechatService sysUserWechatService,
            SysUserService sysUserService,
            SysUserTokenService sysUserTokenService) {
        this.wechatMiniClient = wechatMiniClient;
        this.sysUserWechatService = sysUserWechatService;
        this.sysUserService = sysUserService;
        this.sysUserTokenService = sysUserTokenService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TokenDTO> loginOrRegister(String code) {
        // 1) code 换 openid/unionid
        WechatSession session = wechatMiniClient.code2Session(code);

        if (session.getErrcode() != null && session.getErrcode() != 0) {
            throw new RenException(ErrorCode.WECHAT_CODE_INVALID);
        }
        String openid = session.getOpenid();
        if (openid == null || openid.isBlank()) {
            throw new RenException(ErrorCode.WECHAT_CODE_INVALID);
        }

        // 2) 查映射
        Long userId = sysUserWechatService.getUserId(openid);

        // 3) 没有映射 → 自动“注册”
        if (userId == null) {
            log.info("Auto register user for wechat openid: {}", openid);
            // 3.1 创建 SysUser（沿用你现有 user 系统）
            SysUserDTO user = new SysUserDTO();

            // username 必须唯一：建议用前缀 + openid 或 openid hash
            user.setUsername("wx_" + openid);

            // 重要：生成一个“强密码”来通过 save() 的强度校验
            user.setPassword(randomStrongPassword());

            // 其余字段按你 register 的风格先不管
            sysUserService.save(user);

            // save 后 user.getId() 是否回填取决于你们 Convert/insert 实现
            // 如果 save 不回填 id，那就需要用 username 再查一次 id
            SysUserDTO saved = sysUserService.getByUsername(user.getUsername());
            if (saved == null) {
                throw new RenException(ErrorCode.INVALID_SERVER_ACTION);
            }
            userId = saved.getId();

            // 3.2 写映射
            sysUserWechatService.bind(openid, session.getUnionid(), userId);
        } else {
            log.info("Wechat openid {} mapped to user id {}", openid, userId);
            // 可选：更新 last_login_date、补 unionid
            sysUserWechatService.touch(openid, session.getUnionid());
        }

        // 4) 沿用你们现成 token 体系
        return sysUserTokenService.createToken(userId);
    }

    private static String randomStrongPassword() {
        // 例：16位，包含大小写、数字、符号
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!@#$%^&*()-_=+";

        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        sb.append(upper.charAt(r.nextInt(upper.length())));
        sb.append(lower.charAt(r.nextInt(lower.length())));
        sb.append(digits.charAt(r.nextInt(digits.length())));
        sb.append(symbols.charAt(r.nextInt(symbols.length())));

        String all = upper + lower + digits + symbols;
        for (int i = 0; i < 12; i++) {
            sb.append(all.charAt(r.nextInt(all.length())));
        }

        // 打乱
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }
}
