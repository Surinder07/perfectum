package ca.waaw.dto.appnotifications;

import ca.waaw.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteUserMailDto {

    private User user;

    private String inviteUrl;

}
