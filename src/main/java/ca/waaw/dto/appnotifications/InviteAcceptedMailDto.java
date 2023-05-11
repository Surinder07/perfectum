package ca.waaw.dto.appnotifications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteAcceptedMailDto {

    private String name;

    // Invited user's email to be sent in the mail
    private String userEmail;

    private String location;

    private String role;

    private String langKey;

    private String adminName;

    // Admin's email to send email to
    private String email;

}
