package xiaozhi.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;

@Configuration
@EnableConfigurationProperties(CosProperties.class)
public class CosConfig {

    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(CosProperties properties) {
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        ClientConfig config = new ClientConfig(new Region(properties.getRegion()));
        return new COSClient(credentials, config);
    }
}
