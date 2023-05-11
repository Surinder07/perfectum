package ca.waaw.dto.appnotifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailDto {

    private String email;

    private String name;

    @Builder.Default
    private String langKey = "en";

    private String actionUrl;

    @Builder.Default
    private String buttonText = "Go to WaaW";

    private String websiteUrl;

    private String twitterUrl;

    private String linkedinUrl;

    private String facebookUrl;

    private String instagramUrl;

    private Object message;

    private String title;

    private String organizationName;

}
