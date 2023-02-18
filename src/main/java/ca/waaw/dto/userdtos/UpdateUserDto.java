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
public class UpdateUserDto {

    @NotEmpty
    private String id;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    private String countryCode;

    @Min(value = 1000000000L, message = "Mobile has to be 10 digits")
    @Max(value = 9999999999L, message = "Mobile has to be 10 digits")
    private Long mobile;

    private String country;

    @NotEmpty
    private String locationId;

    @NotEmpty
    private String roleId;

    private boolean isFullTime;

    private String employeeId;

}