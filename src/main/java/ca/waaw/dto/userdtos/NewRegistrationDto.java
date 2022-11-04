package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.ToLowercase;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewRegistrationDto {

    @NotEmpty
    @ToLowercase
    @Size(min = 5, max = 100, message = "Email must be more than 5 characters")
    @ValidateRegex(type = RegexValidatorType.EMAIL, message = "Enter a valid email")
    private String email;

    @NotEmpty
    @Size(min = 8, max = 60, message = "Password must be between 8 and 60 characters")
    @ValidateRegex(type = RegexValidatorType.PASSWORD, message = "Enter a valid password")
    private String password;

    @Schema(description = "Pass true for contractor registration")
    private boolean isContractor;

}
