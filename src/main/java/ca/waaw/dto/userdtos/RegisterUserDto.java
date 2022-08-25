package ca.waaw.dto.userdtos;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterUserDto extends BaseUser {

    @NotEmpty
    private String inviteKey;

    private String employeeId;

}
