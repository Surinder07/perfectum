package ca.waaw.config.applicationconfig;

import ca.waaw.enumration.FileType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@NoArgsConstructor
@ConfigurationProperties("azure")
public class AppAzureConfig {

    private final AppAzureConfig.Sms sms = new AppAzureConfig.Sms();
    private final AppAzureConfig.Blob blob = new AppAzureConfig.Blob();
    private final AppAzureConfig.SendGrid sendGrid = new AppAzureConfig.SendGrid();

    @Data
    @Configuration
    public static class Sms {
        private String endPoint;
        private String keyCredential;
        private String fromMobile;
    }

    @Data
    @Configuration
    public static class Blob {
        private String accountName;
        private String accountKey;
        private String payrollName;
        private String payrollKey;
        private String attendanceName;
        private String attendanceKey;
        private String holidayName;
        private String holidayKey;
        private String picturesName;
        private String picturesKey;
        public String getContainerUrl(FileType type) {
            return "https://" + accountName + ".blob.core.windows.net/" + getContainerName(type);
        }
        public String getContainerName(FileType type) {
            switch (type) {
                case PAYROLL:
                    return payrollName;
                case ATTENDANCE:
                    return attendanceName;
                case HOLIDAYS:
                    return holidayName;
                case PICTURES:
                    return picturesName;
            }
            return null;
        }
        public String getContainerKey(FileType type) {
            switch (type) {
                case PAYROLL:
                    return payrollKey;
                case ATTENDANCE:
                    return attendanceKey;
                case HOLIDAYS:
                    return holidayKey;
                case PICTURES:
                    return picturesKey;
            }
            return null;
        }
    }

    @Data
    @Configuration
    public static class SendGrid {
        private String senderName;
        private String senderEmail;
        private String apiKey;
    }

}
