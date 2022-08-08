package ca.waaw.dto;

import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.ValidatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteUserDto {

    @NotNull
    @ValueOfEnum(enumClass = Authority.class)
    private String role;

    @NotEmpty
    @ValidateRegex(type = ValidatorType.EMAIL)
    private String email;

    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    private String employeeId;

    private String waawRoleId;

    private String locationId;

}
