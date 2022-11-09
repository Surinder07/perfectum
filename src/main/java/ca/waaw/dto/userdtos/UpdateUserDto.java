package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {

    @CapitalizeFirstLetter
    private String firstName;

    @CapitalizeFirstLetter
    private String lastName;

    private String countryCode;

    private String mobile;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

    private Boolean isFullTime;

}