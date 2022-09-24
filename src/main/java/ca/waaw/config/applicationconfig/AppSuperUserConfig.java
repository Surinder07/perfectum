package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.super-user")
public class AppSuperUserConfig {

    private String firstName;

    private String lastName;

    private String email;

    private String username;

    private String password;

    private String organization;

    private String timezone;

}
