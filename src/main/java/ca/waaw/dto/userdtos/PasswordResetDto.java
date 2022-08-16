package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.ValidatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDto {

    @NotEmpty
    @Size(min = 20, max = 20, message = "key is supposed to be 20 characters")
    private String key;

    @NotEmpty
    @Size(min = 8, max = 60, message = "password must be more than 8 characters")
    @ValidateRegex(type = ValidatorType.PASSWORD)
    private String newPassword;

}
