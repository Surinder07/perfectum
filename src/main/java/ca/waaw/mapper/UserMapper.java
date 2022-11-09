package ca.waaw.mapper;

import ca.waaw.domain.EmployeePreferences;
import ca.waaw.domain.Organization;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.*;
import ca.waaw.web.rest.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

public class UserMapper {

    /**
     * @param source User and organization details
     * @return {@link UserDetailsDto} to be sent in response for user details
     */
    public static UserDetailsDto entityToDto(UserOrganization source) {
        UserDetailsDto target = new UserDetailsDto();
        BeanUtils.copyProperties(source, target);
        target.setMobile(CommonUtils.combinePhoneNumber(source.getCountryCode(), source.getMobile()));
        target.setRole(source.getAuthority());
        target.setOrganization(source.getOrganization().getName());
        target.setOrganizationWaawId(source.getOrganization().getWaawId());
        if (source.getAuthority().equals(Authority.ADMIN)) {
            OrganizationPreferences preferences = new OrganizationPreferences();
            preferences.setDaysBeforeShiftsAssigned(source.getOrganization().getDaysBeforeShiftsAssigned());
            preferences.setIsOvertimeRequestEnabled(source.getOrganization().isOvertimeRequestEnabled());
            preferences.setIsTimeclockEnabledDefault(source.getOrganization().isTimeclockEnabledDefault());
            preferences.setIsTimeoffEnabledDefault(source.getOrganization().isTimeoffEnabledDefault());
            target.setOrganizationPreferences(preferences);
        }
        return target;
    }

    /**
     * @param source User and organization details
     * @return {@link UserDetailsNewDto} to be sent in response for user details
     */
    public static UserDetailsNewDto entityToDetailsDto(User source) {
        UserDetailsNewDto target = new UserDetailsNewDto();
        BeanUtils.copyProperties(source, target);
        target.setMobile(CommonUtils.combinePhoneNumber(source.getCountryCode(), source.getMobile()));
        target.setRole(source.getAuthority());
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
        target.setCreatedBy(target.getId());
        target.setLastModifiedBy(target.getId());
        target.setAccountStatus(AccountStatus.EMAIL_PENDING);
        target.setAuthority(source.isContractor() ? Authority.CONTRACTOR : Authority.ADMIN);
        target.setFullTime(!source.isContractor());
        return target;
    }

    /**
     * @param source Complete registration dto
     * @param target User entity to be updated
     * @return Updates the {@link User} object with new provided details and returns a {@link Organization} object
     */
    public static Organization completeRegistrationToEntity(CompleteRegistrationDto source, User target) {
        Organization organization = new Organization();
        organization.setTimezone(source.getTimezone());
        organization.setName(source.getOrganizationName());
        if (StringUtils.isNotEmpty(source.getFirstDayOfWeek()))
            organization.setFirstDayOfWeek(DaysOfWeek.valueOf(source.getFirstDayOfWeek()));
        target.setOrganizationId(organization.getId());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setUsername(source.getUsername());
        target.setLangKey(source.getLangKey());
        target.setCountryCode(source.getCountryCode());
        target.setMobile(String.valueOf(source.getMobile()));
        return organization;
    }

    /**
     * Will update any non-null value in the dto to user entity
     *
     * @param source {@link UpdateUserDto} containing details to update about user
     * @param target {@link User} entity fetched from database to be updated
     */
    public static void updateUserDtoToEntity(UpdateUserDto source, User target) {
        if (StringUtils.isNotEmpty(source.getFirstName())) target.setFirstName(source.getFirstName());
        if (StringUtils.isNotEmpty(source.getLastName())) target.setLastName(source.getLastName());
        if (StringUtils.isNotEmpty(source.getCountryCode())) target.setCountryCode(source.getCountryCode());
        if (StringUtils.isNotEmpty(source.getMobile())) target.setMobile(source.getMobile());
        if (StringUtils.isNotEmpty(source.getLangKey())) target.setLangKey(source.getLangKey());
        if (source.getIsEmailNotifications() != null) target.setEmailNotifications(source.getIsEmailNotifications());
        if (source.getIsSmsNotifications() != null) target.setSmsNotifications(source.getIsSmsNotifications());
        if (source.getIsFullTime() != null) target.setFullTime(source.getIsFullTime());
    }

    /**
     * @param source details for user to be invited
     * @return entity to be saved in database
     */
    public static User inviteUserDtoToEntity(InviteUserDto source) {
        User target = new User();
        target.setEmail(source.getEmail().toLowerCase());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setEmployeeId(source.getEmployeeId());
        target.setLocationId(source.getLocationId());
        target.setLocationRoleId(source.getLocationRoleId());
        if (source.getIsFullTime() != null) target.setFullTime(source.getIsFullTime());
        else target.setFullTime(!source.getRole().equals(Authority.CONTRACTOR.toString()));
        target.setAuthority(Authority.valueOf(source.getRole()));
        target.setAccountStatus(AccountStatus.INVITED);
        return target;
    }

    /**
     * @param source details for user to be mapped
     * @return User details for Admin
     */
    public static UserDetailsForAdminDto entityToUserDetailsForAdmin(UserOrganization source) {
        UserDetailsForAdminDto target = new UserDetailsForAdminDto();
        BeanUtils.copyProperties(source, target);
        target.setRole(source.getAuthority());
        target.setMobile(CommonUtils.combinePhoneNumber(source.getCountryCode(), source.getMobile()));
        target.setLocationId(source.getLocation() == null ? null : source.getLocation().getId());
        target.setLocationName(source.getLocation() == null ? null : source.getLocation().getName());
        target.setLocationRoleId(source.getLocationRole() == null ? null : source.getLocationRole().getId());
        target.setLocationRoleName(source.getLocationRole() == null ? null : source.getLocationRole().getName());
        return target;
    }

    /**
     * @param source details for user to be mapped
     * @return minimal details about user for a drop-down
     */
    public static UserInfoForDropDown entityToUserInfoForDropDown(User source) {
        UserInfoForDropDown target = new UserInfoForDropDown();
        target.setId(source.getId());
        target.setFullName(CommonUtils.combineFirstAndLastName(source.getFirstName(), source.getLastName()));
        target.setEmail(source.getEmail());
        target.setAuthority(source.getAuthority());
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
        if (StringUtils.isNotEmpty(source.getWagesCurrency()))
            target.setWagesCurrency(Currency.valueOf(source.getWagesCurrency()));
        target.setExpired(false);
        return target;
    }

}
