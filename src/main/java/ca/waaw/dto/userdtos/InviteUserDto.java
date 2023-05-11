package ca.waaw.dto.userdtos;

import ca.waaw.enumration.user.Authority;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.LOCATION_ROLE_TO_USER_ROLE,
        message = "Location is required")
public class InviteUserDto {

    @Schema(hidden = true)
    private Authority authority;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.EMAIL, message = "Pass a valid email")
    private String email;

    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    private String employeeId;

    @NotNull
    private Boolean isFullTime;

    private String locationId;

    @NotEmpty
    private String locationRoleId;

}
