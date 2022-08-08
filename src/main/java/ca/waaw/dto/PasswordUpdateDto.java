package ca.waaw.dto;

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
public class PasswordUpdateDto {

    @NotEmpty
    private String oldPassword;

    @NotEmpty
    @Size(min = 8, max = 60, message = "password must be more than 8 characters")
    @ValidateRegex(type = ValidatorType.PASSWORD)
    private String newPassword;

}
