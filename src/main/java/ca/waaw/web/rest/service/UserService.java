package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.Organization;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.NotificationInternalService;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final Logger log = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final AppValidityTimeConfig appValidityTimeConfig;

    private final AppUrlConfig appUrlConfig;

    private final PasswordEncoder passwordEncoder;

    private final UserMailService userMailService;

    private final NotificationInternalService notificationInternalService;

    private final LocationRoleRepository locationRoleRepository;

    /**
     * @param username username to check in database
     * @return true if username is present in database
     */
    public boolean checkIfUsernameExists(String username) {
        return userRepository.findOneByUsernameAndDeleteFlag(username, false).isPresent();
    }

    /**
     * Main User registration method, registration is done through email invite
     *
     * @param userDTO all user related details with invite key
     */
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(RegisterUserDto userDTO) {
        String userId = userRepository.findOneByInviteKey(userDTO.getInviteKey())
                .filter(user -> user.getCreatedDate().isAfter(Instant.now()
                        .minus(appValidityTimeConfig.getUserInvite(), ChronoUnit.DAYS)))
                .map(user -> {
                    UserMapper.updateInvitedUser(userDTO, user);
                    user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
                    return user;
                })
                .map(userRepository::save)
                .map(User::getId)
                .orElseThrow(() -> new ExpiredKeyException("invite"));

        // Sending notification to admin
        userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                .ifPresent(user -> userRepository.findOneByIdAndDeleteFlag(user.getInvitedBy(), false)
                        .ifPresent(admin ->
                                notificationInternalService.notifyAdminAboutNewUser(user, admin, appUrlConfig.getLoginUrl()))
                );
    }

    /**
     * Main User registration method for organizations, admin user account will be created with an organization
     *
     * @param userDTO all user related details with invite key
     */
    @Transactional(rollbackFor = Exception.class)
    public void registerAdminAndOrganization(RegisterOrganizationDto userDTO) {
        checkUserExistence(userDTO);

        User user = UserMapper.registerDtoToUserEntity(userDTO);
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        organization.setCreatedBy(user.getId());
        organization.setLastModifiedBy(user.getId());
        organization.setFirstDayOfWeek(DaysOfWeek.valueOf(userDTO.getFirstDayOfWeek()));
        organization.setName(userDTO.getOrganizationName());
        organization.setOvertimeRequestEnabled(true);
        user.setOrganizationId(organization.getId());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));

        organizationRepository.save(organization);
        userRepository.save(user);
        log.info("New Organization created: {}", organization);
        log.info("new User registered: {}", user);
        String activationUrl = appUrlConfig.getActivateAccountUrl(user.getActivationKey());
        userMailService.sendActivationEmail(user, activationUrl);
    }

    /**
     * Updates the details of current logged-in user
     *
     * @param updateUserDto all user details to update
     */
    public void updateUserDetails(UpdateUserDto updateUserDto) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    UserMapper.updateUserDtoToEntity(updateUserDto, user);
                    return user;
                }).map(userRepository::save)
                .ifPresent(user -> log.info("User successfully updated: {}", user));
    }

    /**
     * Updates the password of logged-in user
     *
     * @param passwordUpdateDto old and new password
     */
    public void updatePasswordOfLoggedInUser(PasswordUpdateDto passwordUpdateDto) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    String currentEncryptedPassword = user.getPasswordHash();
                    if (!passwordEncoder.matches(passwordUpdateDto.getOldPassword(), currentEncryptedPassword)) {
                        throw new AuthenticationException();
                    }
                    String encryptedPassword = passwordEncoder.encode(passwordUpdateDto.getNewPassword());
                    user.setPasswordHash(encryptedPassword);
                    user.setLastModifiedBy(user.getId());
                    return user;
                })
                .map(userRepository::save)
                .ifPresent(user -> log.info("Changed password for User: {}", user));
    }


    /**
     * Used to activate a user when they click link in their mails.
     *
     * @param key activation key received in mail
     */
    public ResponseEntity<String> activateUser(String key) {
        log.info("Activating user for activation key {}", key);
        userRepository.findOneByActivationKey(key)
                .map(user -> {
                    user.setStatus(EntityStatus.ACTIVE);
                    user.setActivationKey(null);
                    user.setLastModifiedDate(Instant.now());
                    return user;
                })
                .map(userRepository::save)
                .orElseThrow(() -> new ExpiredKeyException("activation"));
        String responseTemplate;
        try {
            InputStreamReader stream = new InputStreamReader(new ClassPathResource("templates/mail/SuccessPage.html",
                    this.getClass().getClassLoader()).getInputStream());
            responseTemplate = IOUtils.toString(stream);
        } catch (IOException e) {
            log.error("Error while loading success page, sending plain Success message");
            responseTemplate = "<h1>Success</h1><p>Redirecting you to the login page...</p>";
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Refresh", String.format("2;url=%s", appUrlConfig.getLoginUrl()));
        return new ResponseEntity<>(responseTemplate, httpHeaders, HttpStatus.OK);
    }

    /**
     * If a user accepts invite, this method will fetch their details and redirect to registration page
     *
     * @param key invite key received in mail
     */
    public ResponseEntity<String> acceptInvite(String key) {
        log.info("Getting details for user with invitation key: {}", key);
        String registerUrl = userRepository.findOneByInviteKey(key)
                .map(user -> UserMapper.buildRegisterThroughInviteUrl(user, appUrlConfig.getRegisterUrl()))
                .orElseThrow(() -> new ExpiredKeyException("invitation"));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Refresh", String.format("1;url=%s", registerUrl));
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.OK);
    }

    /**
     * First part of password reset request
     *
     * @param email user email
     */
    public void requestPasswordReset(String email) {
        User user = userRepository
                .findOneByEmailAndDeleteFlag(email, false)
                .filter(localUser -> localUser.getStatus().equals(EntityStatus.ACTIVE))
                .map(localUser -> {
                    localUser.setResetKey(CommonUtils.Random.generateRandomKey());
                    localUser.setResetDate(Instant.now());
                    localUser.setLastModifiedBy(localUser.getId());
                    return localUser;
                })
                .map(userRepository::save)
                .orElseThrow(() -> new EntityNotFoundException("email"));
        String resetUrl = appUrlConfig.getResetPasswordUrl(user.getResetKey());
        userMailService.sendPasswordResetMail(user, resetUrl);
        log.info("Sent a password rest email to {}", email);
    }

    /**
     * Second step of password reset process, Updates the password if the key matches
     *
     * @param passwordResetDto contains reset key and new password
     */
    public void completePasswordReset(PasswordResetDto passwordResetDto) {
        log.debug("Reset user password for reset key {}", passwordResetDto.getKey());
        userRepository
                .findOneByResetKey(passwordResetDto.getKey())
                .filter(user -> user.getResetDate().isAfter(Instant.now()
                        .minus(appValidityTimeConfig.getPasswordReset(), ChronoUnit.DAYS)))
                .map(user -> {
                    user.setPasswordHash(passwordEncoder.encode(passwordResetDto.getNewPassword()));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    user.setLastModifiedBy(user.getId());
                    return user;
                })
                .map(userRepository::save)
                .map(user -> {
                    log.info("Finished reset password request for user : {}", user);
                    return user;
                })
                .orElseThrow(() -> new ExpiredKeyException("password reset"));
    }

    /**
     * Sends an invitation to user email
     *
     * @param inviteUserDto new user details
     */
    public void inviteNewUsers(InviteUserDto inviteUserDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        userRepository.findOneByEmailAndDeleteFlag(inviteUserDto.getEmail(), false)
                .ifPresent(user -> {
                    log.debug("Invited email ({}) is already a member", inviteUserDto.getEmail());
                    throw new EntityAlreadyExistsException("email", inviteUserDto.getEmail());
                });
        User user = UserMapper.inviteUserDtoToEntity(inviteUserDto);
        /*
         * Updating organization id in User object as well as getting the organization name for user invitation mail
         */
        String organizationName = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    user.setInvitedBy(admin.getId());
                    user.setCreatedBy(admin.getId());
                    user.setOrganizationId(admin.getOrganization().getId());
                    return admin.getOrganization().getName();
                })
                .orElseThrow(UnauthorizedException::new);
        userRepository.save(user);
        log.info("New User added to database (pending for accepting invite): {}", user);
        String inviteUrl = appUrlConfig.getInviteUserUrl(user.getInviteKey());
        userMailService.sendInvitationEmail(user, inviteUrl, organizationName);
        log.info("Invitation sent to the user");
    }

    /**
     * @return User details of the logged-in user account
     */
    public UserDetailsDto getLoggedInUserAccount() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(UserMapper::entityToDto)
                )
                .orElseThrow(UnauthorizedException::new);
    }

    /**
     * @return all Employees and Admins under logged-in user
     */
    public PaginationDto getAllUsers(int pageNo, int pageSize, String searchKey, String locationId, String role) {
        CommonUtils.validateStringInEnum(Authority.class, role, "role");
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("firstName", "lastName").ascending());
        Page<UserOrganization> userPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    if (user.getAuthority().equals(Authority.ADMIN) && StringUtils.isNotEmpty(searchKey)) {
                        return userOrganizationRepository.searchUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority("%" + searchKey + "%",
                                user.getOrganizationId(), locationId, false, role, getSortedByName);
                    } else if (user.getAuthority().equals(Authority.ADMIN)) {
                        return userOrganizationRepository.findUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority(user.getOrganizationId(),
                                locationId, false, role, getSortedByName);
                    } else if (StringUtils.isNotEmpty(searchKey)) {
                        return userOrganizationRepository.searchUsersWithLocationIdAndDeleteFlagAndAuthority("%" + searchKey + "%",
                                user.getLocationId(), false, role, getSortedByName);
                    } else {
                        return userOrganizationRepository.findUsersWithLocationIdAndDeleteFlagAndAuthority(user.getLocationId(),
                                false, role, getSortedByName);
                    }
                })
                .orElseThrow(UnauthorizedException::new);
        List<UserDetailsForAdminDto> users = userPage.getContent().stream()
                .map(UserMapper::entityToUserDetailsForAdmin).collect(Collectors.toList());
        return PaginationDto.builder()
                .totalEntries((int) userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .data(users)
                .build();
    }

    /**
     * @return all Employees and Admins for a given location_role_id
     */
    public PaginationDto listAllUsers(int pageNo, int pageSize, String searchKey, String locationRoleId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("firstName", "lastName").ascending());
        LocationRole locationRole = locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                .orElseThrow(() -> new EntityNotFoundException("location role"));

        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    if (user.getOrganizationId().equals(locationRole.getOrganizationId())) return user;
                    return null;
                })
                .orElseThrow(UnauthorizedException::new);

        Page<User> userPage;
        if (StringUtils.isNotEmpty(searchKey)) {
            userPage = userRepository.searchUsersWithLocationRoleIdAndDeleteFlag("%" + searchKey + "%",
                    locationRoleId, false, getSortedByName);
        } else {
            userPage = userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRoleId, false, getSortedByName);
        }
        List<UserInfoForDropDown> users = userPage.getContent().stream()
                .map(UserMapper::entityToUserInfoForDropDown).collect(Collectors.toList());
        return PaginationDto.builder()
                .totalEntries((int) userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .data(users)
                .build();

    }

    /**
     * Updates the preferences of logged-in admins organization
     *
     * @param preferences preferences to be updated
     */
    public void updateOrganizationPreferences(OrganizationPreferences preferences) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false))
                .map(organization -> UserMapper.updateOrganizationPreferences(organization, preferences))
                .map(organization -> CommonUtils.logMessageAndReturnObject(organization, "info", UserService.class,
                        "Organization Preferences for organization id ({}) updated: {}", organization.getId(), preferences))
                .map(organizationRepository::save);
    }

    /**
     * Check if user trying to register already exists (if an unactivated user exists with same details, it is deleted)
     *
     * @param userDTO all user related details
     */
    private void checkUserExistence(RegisterOrganizationDto userDTO) {
        userRepository
                .findOneByUsernameAndDeleteFlag(userDTO.getUsername(), false)
                .ifPresent(existingUser -> {
                    if (existingUser.getStatus().equals(EntityStatus.PENDING)) {
                        userRepository.delete(existingUser);
                    } else {
                        throw new EntityAlreadyExistsException("username", userDTO.getUsername());
                    }
                });
        userRepository.findOneByEmailAndDeleteFlag(userDTO.getEmail(), false)
                .ifPresent(existingUser -> {
                    if (existingUser.getStatus().equals(EntityStatus.PENDING)) {
                        userRepository.delete(existingUser);
                    } else {
                        throw new EntityAlreadyExistsException("email", userDTO.getEmail());
                    }
                });
    }

}
