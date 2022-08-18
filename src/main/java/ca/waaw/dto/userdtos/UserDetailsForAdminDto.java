package ca.waaw.dto.userdtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserDetailsForAdminDto extends BaseUserDetails {

    private String id;

    private String locationId;

    private String locationName;

    private String locationRoleId;

    private String locationRoleName;

}
