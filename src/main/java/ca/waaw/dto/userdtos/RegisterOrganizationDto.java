package ca.waaw.dto.userdtos;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ToLowercase;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
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
    @Size(min = 5, max = 100, message = "email must be more than 5 characters")
    @ValidateRegex(type = RegexValidatorType.EMAIL)
    private String email;

    @NotEmpty
    @CapitalizeFirstLetter
    private String organizationName;

    @Schema(example = "MONDAY")
    @ValueOfEnum(enumClass = DaysOfWeek.class)
    private String firstDayOfWeek;

}
