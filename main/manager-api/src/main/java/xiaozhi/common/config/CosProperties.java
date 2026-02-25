package xiaozhi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucket;
    private String baseUrl;
    private String pathPrefix = "wallpaper/";
}
