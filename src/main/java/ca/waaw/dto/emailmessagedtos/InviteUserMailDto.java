package ca.waaw.dto.emailmessagedtos;

import ca.waaw.domain.User;
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
