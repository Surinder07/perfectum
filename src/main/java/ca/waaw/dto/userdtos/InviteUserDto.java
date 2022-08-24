package ca.waaw.dto.userdtos;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.ROLE_TO_LOCATION_AND_LOCATION_ROLE,
        message = "location_id is required for manager/ both location_id and location_role_id is required for a employee / role has an invalid value")
public class InviteUserDto {

    @NotEmpty
    @Schema(allowableValues = {"ADMIN", "MANAGER", "EMPLOYEE"})
    private String role;

    @NotEmpty
    @ValidateRegex(type = RegexValidatorType.EMAIL)
    private String email;

    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    private String employeeId;

    @Schema(description = "Required if role is <b>EMPLOYEE</b> or <b>MANAGER</b>")
    private String locationId;

    @Schema(description = "Required if role is <b>EMPLOYEE</b>")
    private String locationRoleId;

}
