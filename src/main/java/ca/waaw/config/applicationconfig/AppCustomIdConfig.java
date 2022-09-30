package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.custom-id")
public class AppCustomIdConfig {

    private int length;

    private String userPrefix;

    private String organizationPrefix;

}
