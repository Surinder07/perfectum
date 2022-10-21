package ca.waaw.filehandler;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@NoArgsConstructor
@ConfigurationProperties("files")
public class FileConfig {

    private final FileConfig.FormatsAllowed formatsAllowed = new FileConfig.FormatsAllowed();
    private final FileConfig.RequiredFields requiredFields = new FileConfig.RequiredFields();
    private final FileConfig.PojoTemplates pojoTemplates = new FileConfig.PojoTemplates();

    @Data
    @Configuration
    public static class FormatsAllowed {
        private String[] excel;
        private String[] csv;
    }

    @Data
    @Configuration
    public static class RequiredFields {
        private String[] holidays;
        private String[] inviteUsers;
    }

    @Data
    @Configuration
    public static class PojoTemplates {
        private Map<String, String> holidays;
        private Map<String, String> inviteUsers;
    }

}
