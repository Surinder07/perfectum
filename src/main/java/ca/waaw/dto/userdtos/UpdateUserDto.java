package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {

    private String id;

    private String countryCode;

    @Min(value = 1000000000L, message = "Mobile has to be 10 digits")
    @Max(value = 9999999999L, message = "Mobile has to be 10 digits")
    private Long mobile;

    private String country;

    private String locationId;

    private String roleId;

    private Boolean isFullTime;

}