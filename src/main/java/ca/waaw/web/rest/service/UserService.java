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
import ca.waaw.repository.*;
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
import java.util.UUID;

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
