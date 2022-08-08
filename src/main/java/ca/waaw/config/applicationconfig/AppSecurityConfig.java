package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("security")
public class AppSecurityConfig {

    private String[] unAuthUrlPatterns;

    private String[] corsAllowedOrigins;

    private String jwtSecret;

    private long jwtDefaultTokenValidityInSeconds;

    private long jwtRememberMeTokenValidityInSeconds;

}
