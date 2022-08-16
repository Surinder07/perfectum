package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDto {

    private String employeeId;

    private String waawId;

    private String email;

    private String username;

    private String organization;

    private String organizationWaawId;

    private String firstName;

    private String lastName;

    private String mobile;

    private String role;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

    private Instant lastLogin;

    private List<AccountMessagesDto> accountMessages;

}
