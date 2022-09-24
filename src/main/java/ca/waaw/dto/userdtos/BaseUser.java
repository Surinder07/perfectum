package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ToLowercase;
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
public class BaseUser {

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "Username must be more than 5 characters")
    @ValidateRegex(type = RegexValidatorType.USERNAME, message = "Pass a valid username")
    private String username;

    @NotEmpty
    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    @NotEmpty
    @Size(min = 8, max = 60, message = "Password must be more than 8 characters")
    @ValidateRegex(type = RegexValidatorType.PASSWORD, message = "Pass a valid password")
    private String password;

    private String countryCode;

    private String mobile;

    private String langKey;

}
