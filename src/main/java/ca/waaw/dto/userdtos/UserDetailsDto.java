package ca.waaw.dto.userdtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserDetailsDto extends BaseUserDetails {

    private String organization;

    private String organizationWaawId;

    private OrganizationPreferences organizationPreferences;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

}
