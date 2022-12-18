package ca.waaw.dto.userdtos;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.utils.customannotations.*;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationDto {

    @NotEmpty
    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "Username must be more than 5 characters")
    @ValidateRegex(type = RegexValidatorType.USERNAME, message = "Enter a valid username")
    private String username;

    private String countryCode;

    @Min(value = 1000000000L, message = "Mobile has to be 10 digits")
    @Max(value = 9999999999L, message = "Mobile has to be 10 digits")
    private Long mobile;

    private String country;

    private String langKey;

    @CapitalizeFirstLetter
    private String organizationName;

    @ToUppercase
    @Schema(example = "MONDAY", description = "Required for Admin registration")
    @ValueOfEnum(enumClass = DaysOfWeek.class, message = "Pass correct day of week")
    private String firstDayOfWeek;

    @Schema(description = "Use get Timezones api to get dropdown of possible values, Required for Admin registration")
    @ValueOfEnum(enumClass = Timezones.class, message = "Pass a valid timezone")
    private String timezone;

    @ToUppercase
    @Schema(description = "Required for Admin registration")
    private String promoCode;

}