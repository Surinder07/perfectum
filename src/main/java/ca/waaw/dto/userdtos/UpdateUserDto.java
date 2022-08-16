package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {

    private String firstName;

    private String lastName;

    private String countryCode;

    private String mobile;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

}