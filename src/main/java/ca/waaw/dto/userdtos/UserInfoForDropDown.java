package ca.waaw.dto.userdtos;

import ca.waaw.enumration.Authority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoForDropDown {

    private String id;

    private String fullName;

    private String email;

    private Authority authority;

}