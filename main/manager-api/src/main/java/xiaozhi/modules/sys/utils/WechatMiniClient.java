package xiaozhi.modules.sys.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;

@Component
public class WechatMiniClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wechat.mini.enabled:false}")
    private boolean enabled;

    @Value("${wechat.mini.appid}")
    private String appid;

    @Value("${wechat.mini.secret}")
    private String secret;

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WechatSession code2Session(String code) {
        if (!enabled) {
            String key = (code == null || code.isBlank()) ? "default" : code.trim();
            String h = sha256Hex(key).substring(0, 24);

            WechatSession mock = new WechatSession();
            mock.setOpenid("mock_openid_" + h);
            mock.setUnionid("mock_unionid_" + h);
            mock.setSession_key("mock_session_key_" + h);
            mock.setErrcode(0);
            mock.setErrmsg("ok");
            return mock;
        }

        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        try {
            return restTemplate.getForObject(url, WechatSession.class);
        } catch (Exception e) {
            throw new RenException(ErrorCode.WECHAT_SERVICE_ERROR);
        }
    }
}
