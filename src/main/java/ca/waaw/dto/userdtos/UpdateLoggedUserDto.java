package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoggedUserDto {

    @NotEmpty
    private String firstName;

    private String lastName;

    @Min(value = 1000000000L, message = "Mobile has to be 10 digits")
    @Max(value = 9999999999L, message = "Mobile has to be 10 digits")
    private Long mobile;

    private String country;

    private String countryCode;

}