package ca.waaw.dto.userdtos;

import ca.waaw.enumration.user.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListingDto {

    private String id;

    private String waawId;

    private String email;

    private String fullName;

    private String location;

    private String role;

    private String lastLogin;

    private boolean isFullTime;

    private AccountStatus status;

}