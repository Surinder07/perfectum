package ca.waaw.dto.userdtos;

import ca.waaw.enumration.Authority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseUserDetails {

    private String employeeId;

    private String waawId;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private String mobile;

    private Instant lastLogin;

    private Authority role;
}
