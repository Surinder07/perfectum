package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ToLowercase;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterUserDto extends BaseUser {

    @NotEmpty
    private String inviteKey;

    private String employeeId;

    @NotEmpty
    @ToLowercase
    private String username;

    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    @NotEmpty
    @Size(min = 8, max = 60, message = "password must be more than 8 characters")
    @ValidateRegex(type = RegexValidatorType.PASSWORD)
    private String password;

    private String countryCode;

    private String mobile;

    private String langKey;

}
