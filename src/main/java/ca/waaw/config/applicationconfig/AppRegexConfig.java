package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.validations.regex")
public class AppRegexConfig {

    private String email;

    private String username;

    private String password;

    private String date;

}
