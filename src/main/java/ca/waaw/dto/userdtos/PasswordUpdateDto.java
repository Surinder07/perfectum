package ca.waaw.dto.userdtos;

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
public class PasswordUpdateDto {

    @NotEmpty
    private String oldPassword;

    @NotEmpty
    @Size(min = 8, max = 60, message = "password must be more than 8 characters")
    @ValidateRegex(type = RegexValidatorType.PASSWORD)
    private String newPassword;

}
