package xiaozhi.modules.wallpaper.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WallpaperAuthService {

    private static final String SHARED_SECRET = "在努力之后仍然一事无成的感觉,已经付出一切可还是没有结果的空白,那才是少女要的空,那才是为预言献上的真正祭品";

    public boolean verify(String deviceMac, long ts, String clientSign) {
        long nowSec = System.currentTimeMillis() / 1000;
        if (Math.abs(nowSec - ts) > 24 * 3600) {
            return false;
        }

        // 注意 payload 拼接，要和 esp32 一样
        String payload = deviceMac + ":" + ts;
        String expected = hmacSha256Hex(payload, SHARED_SECRET);

        // 固定时间比较，避免被侧信道搞
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                clientSign.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}
