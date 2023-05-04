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

    private String locationPrefix;

    private String rolePrefix;

    private String requestPrefix;

    private String shiftPrefix;

    private String reportPrefix;

    private String invoicePrefix;

    private String transactionPrefix;

}
