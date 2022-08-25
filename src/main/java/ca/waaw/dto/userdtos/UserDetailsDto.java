package ca.waaw.dto.userdtos;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    private List<AccountMessagesDto> accountMessages = new ArrayList<>();

}
