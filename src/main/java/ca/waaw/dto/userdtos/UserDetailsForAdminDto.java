package ca.waaw.dto.userdtos;

import ca.waaw.enumration.Authority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsForAdminDto {

    private String id;

    private String employeeId;

    private String waawId;

    private String email;

    private Authority authority;

    private String username;

    private String firstName;

    private String lastName;

    private String mobile;

    private Instant lastLogin;

    private String locationId;

    private String locationName;

    private String locationRoleId;

    private String locationRoleName;

}
