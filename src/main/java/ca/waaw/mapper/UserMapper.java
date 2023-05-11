package ca.waaw.mapper;

import ca.waaw.domain.user.EmployeePreferences;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.userdtos.EmployeePreferencesDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.*;
import ca.waaw.enumration.report.PayrollGenerationType;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class UserMapper {

    /**
     * @param source User and organization details
     * @return {@link UserDetailsDto} to be sent in response for user details
     */
    public static UserDetailsDto entityToDto(UserOrganization source) {
        UserDetailsDto target = new UserDetailsDto();
        BeanUtils.copyProperties(source, target);
        target.setImageUrl(source.getImageFile());
        target.setOrganizationLogoUrl(source.getOrganization().getImageFile());
        target.setOrganization(source.getOrganization().getName());
        target.setOrganizationWaawId(source.getOrganization().getWaawId());
        target.setOrganizationTimezone(source.getOrganization().getTimezone());
        target.setTimezone(source.getAuthority().equals(Authority.ADMIN) ? source.getOrganization().getTimezone() :
                source.getLocation().getTimezone());
        target.setRole(source.getAuthority().toString());
        target.setStatus(source.getAccountStatus().toString());
        if (!source.getOrganization().isPlatformFeePaid() && source.getOrganization().getTrialEndDate() != null) {
            target.setTrialDaysPending((int) Duration.between(Instant.now(), source.getOrganization().getTrialEndDate()).toDays());
        }
        target.setStartOfWeek(CommonUtils.capitalizeString(source.getOrganization().getFirstDayOfWeek().toString()));
        if (source.getAuthority().equals(Authority.ADMIN)) {
            OrganizationPreferences preferences = new OrganizationPreferences();
            preferences.setDaysBeforeShiftsAssigned(source.getOrganization().getDaysBeforeShiftsAssigned());
            preferences.setIsOvertimeRequestEnabled(source.getOrganization().isOvertimeRequestEnabled());
            preferences.setIsTimeclockEnabledDefault(source.getOrganization().isTimeclockEnabledDefault());
            preferences.setIsTimeoffEnabledDefault(source.getOrganization().isTimeoffEnabledDefault());
            preferences.setPayrollGenerationFrequency(CommonUtils.capitalizeString(source.getOrganization()
                    .getPayrollGenerationFrequency().toString()));
            preferences.setClockInAllowedMinutesBeforeShift(source.getOrganization().getClockInAllowedMinutesBeforeShift());
            target.setOrganizationPreferences(preferences);
        }
        return target;
    }

    /**
     * @param source User and organization details
     * @return {@link UserListingDto} to be sent in response for user details
     */
    public static UserListingDto entityToDetailsDto(User source) {
        UserListingDto target = new UserListingDto();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * his mapper is used for first time registration Dto for admins with organization
     *
     * @param source {@link NewRegistrationDto} containing new user details
     * @return {@link User} entity to be saved in database
     */
    public static User registerDtoToUserEntity(NewRegistrationDto source) {
        User target = new User();
        BeanUtils.copyProperties(source, target);
        target.setUsername((source.getEmail()));
        target.setCreatedBy(target.getId());
        target.setLastModifiedBy(target.getId());
        target.setAccountStatus(AccountStatus.EMAIL_PENDING);
        target.setAuthority(source.isContractor() ? Authority.CONTRACTOR : Authority.ADMIN);
        target.setFullTime(!source.isContractor());
        return target;
    }

    /**
     * @param source     Complete registration dto
     * @param userTarget User entity to be updated
     */
    public static void completeRegistrationToEntity(CompleteRegistrationDto source, User userTarget) {
        userTarget.setFirstName(source.getFirstName());
        userTarget.setLastName(source.getLastName());
        if (StringUtils.isNotEmpty(source.getUsername())) userTarget.setUsername(source.getUsername());
        userTarget.setCountryCode(source.getCountryCode());
        userTarget.setMobile(String.valueOf(source.getMobile()));
    }

    /**
     * Will update any non-null value in the dto to user entity
     *
     * @param source {@link UpdateUserDto} containing details to update about user
     * @param target {@link User} entity fetched from database to be updated
     */
    public static void updateUserDtoToEntity(UpdateUserDto source, UserOrganization target) {
        if (source.getMobile() != null) {
            target.setMobile(String.valueOf(source.getMobile()));
            target.setCountry(source.getCountry());
            target.setCountryCode(source.getCountryCode());
        }
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setLocationId(source.getLocationId());
        target.setLocationRoleId(source.getRoleId());
        target.setFullTime(source.isFullTime());
        target.setEmployeeId(source.getEmployeeId());
    }

    /**
     * @param source details for user to be invited
     * @return entity to be saved in database
     */
    public static User inviteUserDtoToEntity(InviteUserDto source) {
        User target = new User();
        target.setEmail(source.getEmail().toLowerCase());
        target.setUsername(target.getEmail());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setEmployeeId(source.getEmployeeId());
        target.setLocationId(source.getLocationId());
        target.setLocationRoleId(source.getLocationRoleId());
        target.setFullTime(source.getIsFullTime());
        target.setAuthority(source.getAuthority());
        target.setAccountStatus(AccountStatus.INVITED);
        return target;
    }

    /**
     * @param source details for user to be mapped
     * @return User details for Admin
     */
    public static UserListingDto entityToUserDetailsForListing(UserOrganization source, String timezone) {
        UserListingDto target = new UserListingDto();
        BeanUtils.copyProperties(source, target);
        target.setFullName(source.getFullName());
        target.setLocation(source.getLocation() == null ? "-" : source.getLocation().getName());
        target.setRole(source.getLocationRole() == null ? "-" : source.getLocationRole().getName());
        target.setLastLogin(source.getLastLogin() == null ? "-" : DateAndTimeUtils.getFullMonthDateWithTime(source.getLastLogin(), timezone));
        target.setFullTime(source.isFullTime());
        target.setStatus(source.getAccountStatus());
        return target;
    }

    /**
     * @param userSource        user entity
     * @param preferencesSource employee preference
     * @return data to be shown to admin for an employee
     */
    public static UserDetailsForAdminDto entityToUserDetailsForAdmin(UserOrganization userSource, EmployeePreferences preferencesSource) {
        UserDetailsForAdminDto target = new UserDetailsForAdminDto();
        BeanUtils.copyProperties(userSource, target);
        target.setImageUrl(userSource.getImageFile());
        target.setLocationName(userSource.getLocation().getName());
        target.setLocationRoleName(userSource.getLocationRole().getName());
        target.setFullTime(userSource.isFullTime());
        target.setEmployeePreferences(preferencesSource == null ? new EmployeePreferencesDto() : employeePreferenceToDto(preferencesSource));
        return target;
    }

    /**
     * Updates preferences if not null
     *
     * @param target Organization entity to be saved in database
     * @param source Preferences to be updated in entity
     * @return Same Organization entity
     */
    public static Organization updateOrganizationPreferences(Organization target, OrganizationPreferences source) {
        if (source.getIsOvertimeRequestEnabled() != null)
            target.setOvertimeRequestEnabled(source.getIsOvertimeRequestEnabled());
        if (source.getIsTimeclockEnabledDefault() != null)
            target.setTimeclockEnabledDefault(source.getIsTimeclockEnabledDefault());
        if (source.getIsTimeoffEnabledDefault() != null)
            target.setTimeoffEnabledDefault(source.getIsTimeoffEnabledDefault());
        if (source.getDaysBeforeShiftsAssigned() != null)
            target.setDaysBeforeShiftsAssigned(source.getDaysBeforeShiftsAssigned());
        if (source.getPayrollGenerationFrequency() != null) {
            target.setPayrollGenerationFrequency(PayrollGenerationType.valueOf(source.getPayrollGenerationFrequency().toUpperCase()));
        }
        target.setClockInAllowedMinutesBeforeShift(source.getClockInAllowedMinutesBeforeShift());
        return target;
    }

    // Employee related mapping

    /**
     * @param source employee preference database entity
     * @return dto containing preference info
     */
    public static EmployeePreferencesDto employeePreferenceToDto(EmployeePreferences source) {
        EmployeePreferencesDto target = new EmployeePreferencesDto();
        BeanUtils.copyProperties(source, target);
        target.setWagesCurrency(source.getWagesCurrency() == null ? null : source.getWagesCurrency().toString());
        target.setActive(!source.isExpired());
        return target;
    }

    /**
     * @param source employee preference dto
     * @return employee preference entity to save in the database
     */
    public static EmployeePreferences employeePreferencesToEntity(EmployeePreferencesDto source) {
        EmployeePreferences target = new EmployeePreferences();
        BeanUtils.copyProperties(source, target);
        target.setId(UUID.randomUUID().toString());
        if (StringUtils.isNotEmpty(source.getWagesCurrency()))
            target.setWagesCurrency(Currency.valueOf(source.getWagesCurrency()));
        target.setExpired(false);
        return target;
    }

}
