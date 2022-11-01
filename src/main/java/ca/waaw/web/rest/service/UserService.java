package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.Organization;
import ca.waaw.domain.PromotionCode;
import ca.waaw.domain.User;
import ca.waaw.domain.UserTokens;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.enumration.PromoCodeType;
import ca.waaw.enumration.UserToken;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.NotificationInternalService;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.HtmlTemplatesPath;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class UserService {

    private final Logger log = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final UserTokenRepository userTokenRepository;

    private final OrganizationRepository organizationRepository;

    private final PromotionCodeRepository promotionCodeRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final AppValidityTimeConfig appValidityTimeConfig;

    private final AppUrlConfig appUrlConfig;

    private final PasswordEncoder passwordEncoder;

    private final UserMailService userMailService;

    private final NotificationInternalService notificationInternalService;

    private final AppCustomIdConfig appCustomIdConfig;

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
    // TODO Change to accept invite
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(RegisterUserDto userDTO) {
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(userDTO.getInviteKey(),
                        UserToken.INVITE, false)
                .map(token -> {
                    if (token.getCreatedDate().isAfter(Instant.now()
                            .minus(appValidityTimeConfig.getUserInvite(), ChronoUnit.DAYS))) {
                        token.setExpired(true);
                        userTokenRepository.save(token);
                        return null;
                    }
                    return token;
                })
                .flatMap(token -> userOrganizationRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> {
                            String currentCustomId = userRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getUserPrefix() + "0000000000");
                            UserMapper.updateInvitedUser(userDTO, user);
                            user.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
                            user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
                            return user;
                        })
                        .map(userOrganizationRepository::save)
                        .flatMap(user -> userRepository.findOneByIdAndDeleteFlag(token.getCreatedBy(), false)
                                .map(admin -> {
                                    // Sending notification to admin
                                    notificationInternalService.notifyAdminAboutNewUser(user, admin, appUrlConfig.getLoginUrl());
                                    return user;
                                })
                        )
                )
                .orElseThrow(() -> new ExpiredKeyException("invite"));
    }

    // TODO Split into two APIs for simple registration and complete registration
    /**
     * Main User registration method for organizations, admin user account will be created with an organization
     *
     * @param userDTO all user related details with invite key
     */
    @Transactional(rollbackFor = Exception.class)
    public void registerAdminAndOrganization(RegisterOrganizationDto userDTO) {
        checkUserExistence(userDTO);
        int trialDays = 0;
        if (StringUtils.isNotEmpty(userDTO.getPromoCode())) {
            trialDays = promotionCodeRepository.findOneByCodeAndTypeAndDeleteFlag(userDTO.getPromoCode(), PromoCodeType.TRIAL, false)
                    .map(PromotionCode::getPromotionValue)
                    .orElseThrow(() -> new EntityNotFoundException("promo code"));
        }

        String currentCustomId = userRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getUserPrefix() + "0000000000");
        String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getOrganizationPrefix() + "0000000000");

        User user = UserMapper.registerDtoToUserEntity(userDTO);
        Organization organization = new Organization();
        organization.setCreatedBy(user.getId());
        organization.setLastModifiedBy(user.getId());
        organization.setFirstDayOfWeek(DaysOfWeek.valueOf(userDTO.getFirstDayOfWeek()));
        organization.setName(userDTO.getOrganizationName());
        organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
        organization.setTimezone(userDTO.getTimezone());
        organization.setTrialDays(trialDays);
        user.setOrganizationId(organization.getId());
        user.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        UserTokens token = new UserTokens(UserToken.ACTIVATION);
        token.setUserId(user.getId());
        token.setCreatedBy("SYSTEM");

        organizationRepository.save(organization);
        userRepository.save(user);
        userTokenRepository.save(token);
        log.info("New Organization created: {}", organization);
        log.info("New User registered: {}", user);
        log.info("New activation token generated: {}", token);
        String activationUrl = appUrlConfig.getActivateAccountUrl(token.getToken());
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
    // TODO Change to complete profile with another api to check activation key
    public ResponseEntity<String> activateUser(String key) {
        log.info("Activating user for activation key {}", key);
        String errorMessage = "Your activation link has been expired.";
        HttpHeaders httpHeaders = new HttpHeaders();
        String responseTemplate = userTokenRepository
                .findOneByTokenAndTokenTypeAndIsExpired(key, UserToken.ACTIVATION, false)
                .map(userTokens -> {
                    InputStreamReader stream;
                    if (userTokens.getCreatedDate().isAfter(Instant.now()
                            .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))) {
                        userTokens.setExpired(true);
                        userTokenRepository.save(userTokens);
                        try {
                            stream = new InputStreamReader(new ClassPathResource(HtmlTemplatesPath.errorPage,
                                    this.getClass().getClassLoader()).getInputStream());
                            return IOUtils.toString(stream).replace("messageBody", errorMessage)
                                    .replace("titleMessage", "Link Expired")
                                    .replace("homepageUrl", appUrlConfig.getRegisterUrl());
                        } catch (Exception e) {
                            return "<h1>Activation key is expired</h1>";
                        }
                    } else {
                        httpHeaders.add("Refresh", String.format("2;url=%s", appUrlConfig.getLoginUrl()));
                        userRepository.findOneByIdAndDeleteFlag(userTokens.getUserId(), false)
                                .map(user -> {
                                    user.setStatus(EntityStatus.ACTIVE);
                                    user.setLastModifiedDate(Instant.now());
                                    return user;
                                })
                                .map(userRepository::save);
                        try {
                            stream = new InputStreamReader(new ClassPathResource(HtmlTemplatesPath.successPage,
                                    this.getClass().getClassLoader()).getInputStream());
                            return IOUtils.toString(stream).replace("messageBody", "Your account was successfully activated")
                                    .replace("titleMessage", "Account activated")
                                    .replace("homepageUrl", appUrlConfig.getLoginUrl());
                        } catch (Exception e) {
                            return "<h1>Success</h1><p>Redirecting you to the login page...</p>";
                        }
                    }
                })
                .orElseGet(() -> {
                    try {
                        InputStreamReader stream = new InputStreamReader(new ClassPathResource(HtmlTemplatesPath.errorPage,
                                this.getClass().getClassLoader()).getInputStream());
                        return IOUtils.toString(stream).replace("messageBody", errorMessage)
                                .replace("titleMessage", "Link Expired")
                                .replace("homepageUrl", appUrlConfig.getRegisterUrl());
                    } catch (Exception e) {
                        return "<h1>Activation key is expired</h1>";
                    }
                });
        return new ResponseEntity<>(responseTemplate, httpHeaders, HttpStatus.OK);
    }

    /**
     * If a user accepts invite, this method will fetch their details and redirect to registration page
     *
     * @param key invite key received in mail
     */
    // TODO merge accept invite with register and create separate api for checking invite key
    public ResponseEntity<String> acceptInvite(String key) {
        log.info("Getting details for user with invitation key: {}", key);
        String registerUrl = userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(key, UserToken.INVITE, false)
                .map(token -> {
                    if (token.getCreatedDate().isAfter(Instant.now()
                            .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))) {
                        token.setExpired(true);
                        userTokenRepository.save(token);
                        return null;
                    }
                    return token;
                })
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> UserMapper.buildRegisterThroughInviteUrl(user, appUrlConfig.getRegisterUrl(), token.getToken()))
                ).orElse(null);
        String body = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        if (registerUrl == null) {
            try {
                String message = "Your invitation url seems to be expired, Please contact admin.";
                InputStreamReader stream = new InputStreamReader(new ClassPathResource(HtmlTemplatesPath.errorPage,
                        this.getClass().getClassLoader()).getInputStream());
                body = IOUtils.toString(stream).replace("messageBody", message)
                        .replace("titleMessage", "Link Expired")
                        .replace("homepageUrl", appUrlConfig.getHostedUi());
            } catch (Exception e) {
                body = "<h1>Invitation key is expired</h1>";
            }
        } else {
            httpHeaders.add("Refresh", String.format("1;url=%s", registerUrl));
        }
        return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
    }

    /**
     * First part of password reset request
     *
     * @param email user email
     */
    public void requestPasswordReset(String email) {
        userRepository
                .findOneByEmailAndDeleteFlag(email, false)
                .filter(localUser -> localUser.getStatus().equals(EntityStatus.ACTIVE))
                .map(user -> {
                    UserTokens token = new UserTokens(UserToken.RESET);
                    token.setUserId(user.getId());
                    token.setCreatedBy("SYSTEM");
                    userTokenRepository.save(token);
                    String resetUrl = appUrlConfig.getResetPasswordUrl(token.getToken());
                    userMailService.sendPasswordResetMail(user, resetUrl);
                    return user;
                })
                .orElseThrow(() -> new EntityNotFoundException("email"));
        log.info("Sent a password rest email to {}", email);
    }

    /**
     * Second step of password reset process, Updates the password if the key matches
     *
     * @param passwordResetDto contains reset key and new password
     */
    public void completePasswordReset(PasswordResetDto passwordResetDto) {
        log.debug("Reset user password for reset key {}", passwordResetDto.getKey());
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(passwordResetDto.getKey(), UserToken.RESET,
                        false)
                .map(token -> {
                    if (token.getCreatedDate().isAfter(Instant.now()
                            .minus(appValidityTimeConfig.getPasswordReset(), ChronoUnit.DAYS))) {
                        token.setExpired(true);
                        userTokenRepository.save(token);
                        throw new ExpiredKeyException("password reset");
                    }
                    return token;
                })
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false))
                .map(user -> {
                    user.setPasswordHash(passwordEncoder.encode(passwordResetDto.getNewPassword()));
                    user.setLastModifiedBy(user.getId());
                    return user;
                })
                .map(userRepository::save)
                .map(user -> CommonUtils.logMessageAndReturnObject(user, "info", UserService.class,
                            "Finished reset password request for user : {}", user))
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
