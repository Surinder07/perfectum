package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDto {

    private String employeeId;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private String mobile;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

    private Instant lastLogin;

}
