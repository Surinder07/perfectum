package ca.waaw.dto.userdtos;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.Timezones;
import ca.waaw.web.rest.utils.customannotations.*;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterOrganizationDto extends BaseUser {

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "Email must be more than 5 characters")
    @ValidateRegex(type = RegexValidatorType.EMAIL, message = "Enter a valid email")
    private String email;

    @NotEmpty
    @CapitalizeFirstLetter
    private String organizationName;

    @ToUppercase
    @Schema(example = "MONDAY")
    @ValueOfEnum(enumClass = DaysOfWeek.class, message = "Pass correct day of week")
    private String firstDayOfWeek;

    @NotEmpty
    @Schema(description = "Use get Timezones api to get dropdown of possible values")
    @ValueOfEnum(enumClass = Timezones.class, message = "Pass a valid timezone")
    private String timezone;

}
