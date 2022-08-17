package ca.waaw.mapper;

import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.web.rest.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

public class UserMapper {

    /**
     * @param source User and organization details
     * @return {@link UserDetailsDto} to be sent in response for user details
     */
    public static UserDetailsDto entityToDto(UserOrganization source) {
        UserDetailsDto target = new UserDetailsDto();
        BeanUtils.copyProperties(source, target);
        target.setMobile(source.getCountryCode() + "-" + target.getMobile());
        target.setRole(source.getAuthority().toString());
        target.setOrganization(source.getOrganization().getName());
        target.setOrganizationWaawId(source.getOrganization().getWaawId());
        return target;
    }

    /**
     * This mapper is used for first time registration Dto for invited users
     *
     * @param source {@link RegisterUserDto} containing new user details
     * @param target {@link User} entity to be saved in database
     */
    public static void updateInvitedUser(RegisterUserDto source, User target) {
        target.setLastModifiedBy(target.getId());
        target.setUsername(source.getUsername());
        if (StringUtils.isNotEmpty(source.getFirstName())) target.setFirstName(source.getFirstName());
        if (StringUtils.isNotEmpty(source.getLastName())) target.setLastName(source.getLastName());
        target.setMobile(source.getMobile());
        target.setCountryCode(source.getCountryCode());
        target.setLangKey(source.getLangKey());
        target.setStatus(EntityStatus.ACTIVE);
    }

    /**
     * his mapper is used for first time registration Dto for admins with organization
     *
     * @param source {@link RegisterOrganizationDto} containing new user details
     * @return {@link User} entity to be saved in database
     */
    public static User registerDtoToUserEntity(RegisterOrganizationDto source) {
        User target = new User();
        BeanUtils.copyProperties(source, target);
        target.setId(UUID.randomUUID().toString());
        target.setCreatedBy(target.getId());
        target.setLastModifiedBy(target.getId());
        target.setStatus(EntityStatus.PENDING);
        target.setAuthority(Authority.ADMIN);
        target.setActivationKey(CommonUtils.Random.generateRandomKey());
        return target;
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
    }

    /**
     * @param source details for user to be invited
     * @return entity to be saved in database
     */
    public static User inviteUserDtoToEntity(InviteUserDto source) {
        User target = new User();
        target.setId(UUID.randomUUID().toString());
        target.setEmail(source.getEmail());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setEmployeeId(source.getEmployeeId());
        target.setLocationId(source.getLocationId());
        target.setLocationRoleId(source.getLocationRoleId());
        target.setAuthority(Authority.valueOf(source.getRole()));
        target.setStatus(EntityStatus.PENDING);
        target.setInviteKey(CommonUtils.Random.generateRandomKey());
        return target;
    }

    /**
     * @param source details for user to be invited
     * @return entity to be saved in database
     */
    public static UserDetailsForAdminDto entityToUserDetailsForAdmin(UserOrganization source) {
        UserDetailsForAdminDto target = new UserDetailsForAdminDto();
        BeanUtils.copyProperties(source, target);
        target.setMobile(source.getCountryCode() + " " + source.getMobile());
        target.setLocationId(source.getLocation().getId());
        target.setLocationName(source.getLocation().getName());
        target.setLocationRoleId(source.getLocationRole().getId());
        target.setLocationRoleName(source.getLocationRole().getName());
        return target;
    }

    /**
     * @param user            new user to register
     * @param registrationUrl Registration url set in {@code application.yml}
     * @return Registration url with known user info as query params
     */
    public static String buildRegisterThroughInviteUrl(User user, String registrationUrl) {
        return String.format(
                "%s?key=%s" + (StringUtils.isNotEmpty(user.getFirstName()) ? "?firstName=%s" : "%s")
                        + (StringUtils.isNotEmpty(user.getLastName()) ? "?lastName=%s" : "%s"),
                registrationUrl, user.getInviteKey(), StringUtils.isNotEmpty(user.getFirstName()) ? user.getFirstName() : "",
                StringUtils.isNotEmpty(user.getLastName()) ? user.getLastName() : ""
        );
    }

}
