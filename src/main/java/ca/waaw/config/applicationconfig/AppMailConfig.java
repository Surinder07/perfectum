package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.mail")
public class AppMailConfig {

    private String twitterUrl;

    private String linkedInUrl;

    private String uiUrl;

    private String adminEmail;

    private String senderEmail;

    private String senderName;

}
