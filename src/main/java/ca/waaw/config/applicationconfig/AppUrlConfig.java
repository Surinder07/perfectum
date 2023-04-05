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

    private String updateEmail;

    private String login;

    public String getHostedUi() {
        return hostedUi;
    }

    public String getHostedServer() {
        return hostedServer;
    }

    public String getActivateAccountUrl(String key) {
        return String.format("%s%s%s", hostedUi, activateAccount, key);
    }

    public String getResetPasswordUrl(String key) {
        return String.format("%s%s%s", hostedUi, resetPassword, key);
    }

    public String getInviteUserUrl(String key) {
        return String.format("%s%s%s", hostedUi, inviteUser, key);
    }

    public String getLoginUrl() {
        return String.format("%s%s", hostedUi, login);
    }

    public String getUpdateEmailUrl(String key) {
        return String.format("%s%s%s", hostedUi, updateEmail, key);
    }

    public String getImageUrl(String id, String imageType) {
        return String.format("%s/api/v1/unAuth/resource/image/%s/%s", getHostedServer(), imageType.toLowerCase(), id);
    }

}
