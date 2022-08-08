package ca.waaw.dto;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ToLowercase;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.ValidatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOrganizationDto {

    @NotEmpty
    @CapitalizeFirstLetter
    private String organizationName;

    @ValueOfEnum(enumClass = DaysOfWeek.class)
    private String firstDayOfWeek;

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "email must be more than 5 characters")
    @ValidateRegex(type = ValidatorType.EMAIL)
    private String email;

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "username must be more than 5 characters")
    @ValidateRegex(type = ValidatorType.USERNAME)
    private String username;

    @NotEmpty
    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    @NotEmpty
    @Size(min = 8, max = 60, message = "password must be more than 8 characters")
    @ValidateRegex(type = ValidatorType.PASSWORD)
    private String password;

    private String countryCode;

    private String mobile;

    private String langKey;

}
