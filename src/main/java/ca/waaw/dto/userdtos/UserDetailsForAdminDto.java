package ca.waaw.dto.userdtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsForAdminDto {

    private String id;

    private  String firstName;

    private String lastName;

    private String email;

    private String imageUrl;

    private String country;

    private String countryCode;

    private String mobile;

    private String employeeId;

    private String waawId;

    private String locationId;

    private String locationName;

    private String locationRoleId;

    private String locationRoleName;

    private boolean isFullTime;

    private EmployeePreferencesDto employeePreferences;

}
