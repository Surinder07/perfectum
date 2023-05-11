package ca.waaw.dto.userdtos;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDto {

    private String id;

    private String employeeId;

    private String waawId;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private String imageUrl;

    private String mobile;

    private String countryCode;

    private String country;

    private Instant lastLogin;

    private String organizationId;

    private String organization;

    private String organizationWaawId;

    private String organizationLogoUrl;

    private String organizationTimezone;

    private String timezone;

    private String role;

    private String startOfWeek;

    private OrganizationPreferences organizationPreferences;

    private String langKey;

    private Boolean isEmailNotifications;

    private Boolean isSmsNotifications;

    private String status;

    private int trialDaysPending; // TODO send seconds to ui and calculate time there to update properly

}
