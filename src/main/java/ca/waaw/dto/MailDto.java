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

    private String langKey;

    private String actionUrl;

    private String websiteUrl;

    private String twitterUrl;

    private String linkedinUrl;

    private Object message;

    private String organizationName;

}
