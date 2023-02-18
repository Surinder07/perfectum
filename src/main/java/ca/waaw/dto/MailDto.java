package ca.waaw.dto;

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

    private String langKey;

    private String actionUrl;

    private String websiteUrl;

    private String twitterUrl;

    private String linkedinUrl;

    private String facebookUrl;

    private String instagramUrl;

    private Object message;

    private String title;

    private String organizationName;

}
