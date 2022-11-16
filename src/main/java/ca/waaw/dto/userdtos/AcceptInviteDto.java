package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInviteDto {

    @NotEmpty
    private String inviteKey;

    @NotEmpty
    @Size(min = 8, max = 60, message = "Password must be between 8 and 60 characters")
    @ValidateRegex(type = RegexValidatorType.PASSWORD, message = "Enter a valid password")
    private String password;

}