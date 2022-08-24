package ca.waaw.dto.userdtos;

import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValidateLocationAndRole;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.LocationRoleValidatorType;
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
@ValidateLocationAndRole(type = LocationRoleValidatorType.ROLE_TO_LOCATION_AND_LOCATION_ROLE)
public class InviteUserDto {

    @NotNull
    @Schema(allowableValues = {"ADMIN", "MANAGER", "EMPLOYEE"})
    @ValueOfEnum(enumClass = Authority.class, message = "Please pass a valid role")
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
