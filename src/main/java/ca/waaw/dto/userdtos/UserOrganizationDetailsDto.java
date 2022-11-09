package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationDetailsDto {

    private String organization;

    private String organizationWaawId;

    private OrganizationPreferences organizationPreferences;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

}