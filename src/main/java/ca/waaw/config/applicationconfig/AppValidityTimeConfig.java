package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.validity")
public class AppValidityTimeConfig {

    private int activationLink;
    private int passwordReset;
    private int userInvite;
    private int emailUpdate;

}
