package ca.waaw.dto.userdtos;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserDetailsDto extends BaseUserDetails {

    private String organization;

    private String organizationWaawId;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

    private List<AccountMessagesDto> accountMessages;

}
