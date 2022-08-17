package ca.waaw.config.applicationconfig;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Configuration
@ConfigurationProperties("application.urls")
public class AppUrlConfig {

    private String hostedUi;
    private String hostedServer;
    private String activateAccount;
    private String resetPassword;
    private String inviteUser;
    private String register;
    private String login;

    public String getActivateAccountUrl(String key) {
        return String.format("%s%s%s", hostedServer, activateAccount, key);
    }

    public String getResetPasswordUrl(String key) {
        return String.format("%s%s%s", hostedUi, resetPassword, key);
    }

    public String getInviteUserUrl(String key) {
        return String.format("%s%s%s", hostedServer, inviteUser, key);
    }

    public String getRegisterUrl() {
        return String.format("%s%s", hostedUi, register);
    }

    public String getLoginUrl() {
        return String.format("%s%s", hostedUi, login);
    }

}
