package ca.waaw.config.applicationconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("application.websockets")
public class WebSocketConfig {

    private String connectionEndpoint;

    private String simpleBroker;

    private String applicationDestinationPrefix;

    private String[] allowedOrigins;

    private String notificationUrl;

    private String shiftUrl;

    private String timesheetUrl;

    private String userInviteUrl;

    private String holidayUploadUrl;

    private String updateUserDetailUrl;

}
