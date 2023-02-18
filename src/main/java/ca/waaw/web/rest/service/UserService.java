package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.*;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.*;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.*;
import ca.waaw.repository.joined.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.service.NotificationInternalService;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserService {

    private final Logger log = LogManager.getLogger(UserService.class);

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final UserTokenRepository userTokenRepository;

    private final OrganizationRepository organizationRepository;

    private final PromotionCodeRepository promotionCodeRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final EmployeePreferencesRepository employeePreferencesRepository;

    private final AppValidityTimeConfig appValidityTimeConfig;

    private final AppUrlConfig appUrlConfig;

    private final PasswordEncoder passwordEncoder;

    private final UserMailService userMailService;

    private final NotificationInternalService notificationInternalService;

    private final AppCustomIdConfig appCustomIdConfig;

    @Transactional(rollbackFor = Exception.class)
    public void registerNewUser(NewRegistrationDto registrationDto) {
        userRepository.findOneByEmailAndDeleteFlag(registrationDto.getEmail(), false)
                .ifPresent(user -> {
                    if (user.getAccountStatus().equals(AccountStatus.EMAIL_PENDING)) {
                        user.setDeleteFlag(true);
                        userRepository.save(user);
                    } else
                        throw new EntityAlreadyExistsException("email", "address", registrationDto.getEmail());
                });
        String currentCustomId = userRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getUserPrefix() + "0000000000");
        User user = UserMapper.registerDtoToUserEntity(registrationDto);
        user.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        UserTokens token = new UserTokens(UserTokenType.ACTIVATION);
        token.setUserId(user.getId());
        token.setCreatedBy("SYSTEM");
        userRepository.save(user);
        userTokenRepository.save(token);
        log.info("New User registered: {}", user);
        log.info("New activation token generated: {}", token);
        String activationUrl = appUrlConfig.getActivateAccountUrl(token.getToken());
        // @todo email and change activation url
        userMailService.sendVerificationEmail(user, activationUrl);
    }

    /**
     * Used to activate a user by verifying email when they click link in their mails.
     *
     * @param verificationKey activation verificationKey received in mail
     */
    public void verifyEmail(String verificationKey) {
        log.info("Activating user for verificationKey {}", verificationKey);
        userTokenRepository
                .findOneByTokenAndTokenTypeAndIsExpired(verificationKey, UserTokenType.ACTIVATION, false)
                .flatMap(userTokens -> {
                    if (userTokens.getCreatedDate().isBefore(Instant.now()
                            .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))) {
                        userTokens.setExpired(true);
                        userTokenRepository.save(userTokens);
                        throw new ExpiredKeyException("verification");
                    } else {
                        return userRepository.findOneByIdAndDeleteFlag(userTokens.getUserId(), false)
                                .map(user -> {
                                    user.setAccountStatus(AccountStatus.PROFILE_PENDING);
                                    user.setLastModifiedDate(Instant.now());
                                    log.info("Saving new User: {}", user);
                                    return user;
                                })
                                .map(userRepository::save)
                                .map(user -> CommonUtils.logMessageAndReturnObject(user, "info", UserService.class,
                                        "New User registered: {}", user));
                    }
                })
                .orElseThrow(() -> new ExpiredKeyException("verification"));
    }

    /**
     * Complete registration after email verification
     *
     * @param completeRegistrationDto Complete registration details
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto completeRegistration(CompleteRegistrationDto completeRegistrationDto) {
        User loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> {
                            if (user.getAuthority().equals(Authority.ADMIN)) {
                                if (StringUtils.isEmpty(completeRegistrationDto.getOrganizationName())) {
                                    throw new BadRequestException("Organization Name is required.", "Organization.name");
                                }
                                String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                                        .orElse(appCustomIdConfig.getOrganizationPrefix() + "0000000000");
                                int trialDays = 0;
                                if (StringUtils.isNotEmpty(completeRegistrationDto.getPromoCode())) {
                                    trialDays = promotionCodeRepository.findOneByCodeAndTypeAndDeleteFlag(completeRegistrationDto.getPromoCode(),
                                                    PromoCodeType.TRIAL, false)
                                            .map(PromotionCode::getPromotionValue)
                                            .orElseThrow(() -> new EntityNotFoundException("promo code", completeRegistrationDto.getPromoCode()));
                                }
                                Organization organization = new Organization();
                                organization.setName(completeRegistrationDto.getOrganizationName());
                                organization.setTimezone(completeRegistrationDto.getTimezone());
                                if (StringUtils.isNotEmpty(completeRegistrationDto.getFirstDayOfWeek())) {
                                    organization.setFirstDayOfWeek(DaysOfWeek.valueOf(completeRegistrationDto.getFirstDayOfWeek()));
                                }
                                organization.setTrialDays(trialDays);
                                organization.setCreatedBy(user.getId());
                                organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
                                organizationRepository.save(organization);
                                user.setOrganizationId(organization.getId());
                            }
                            UserMapper.completeRegistrationToEntity(completeRegistrationDto, user);
                            return user;
                        })
                        .map(userRepository::save)
                )
                .orElseThrow(AuthenticationException::new);
        // Updating jwt with the new username
        final String jwt = tokenProvider.updateUsernameOrStatusInToken(loggedUser.getUsername(), loggedUser.getAccountStatus());
        return new LoginResponseDto(jwt);
    }

    /**
     * @param promoCode promo value
     * @return Message for applied perk or error
     */
    public Map<String, String> validatePromoCode(String promoCode) {
        return promotionCodeRepository.findOneByCodeAndDeleteFlag(promoCode, false)
                .map(code -> {
                    if (code.getExpiryDate() != null && code.getExpiryDate().isBefore(Instant.now())) {
                        return null;
                    }
                    Map<String, String> response = new HashMap<>();
                    if (code.getType().equals(PromoCodeType.TRIAL)) {
                        response.put("message", String.format("Trial Period for %s days will be added to your account.", code.getPromotionValue()));
                    }
                    return response;
                })
                .orElseThrow(() -> new EntityNotFoundException("promotion code"));
    }

    /**
     * If a user accepts invite, this method will fetch their details
     *
     * @param invitationKey invite key received in mail
     */
    public UserListingDto checkInviteLink(String invitationKey) {
        log.info("Getting details for user with invitation key: {}", invitationKey);
        return userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(invitationKey, UserTokenType.INVITE, false)
                .map(token -> {
                    if (token.getCreatedDate().isBefore(Instant.now()
                            .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))) {
                        token.setExpired(true);
                        userTokenRepository.save(token);
                        log.info("Invitation key expired: {}", invitationKey);
                        throw new ExpiredKeyException("invitation");
                    }
                    return token;
                })
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(UserMapper::entityToDetailsDto)
                ).orElseThrow(() -> new ExpiredKeyException("invitation"));
    }

    /**
     * Complete registration for invited users
     *
     * @param acceptInviteDto invite key and newPassword info
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvite(AcceptInviteDto acceptInviteDto) {
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(acceptInviteDto.getInviteKey(), UserTokenType.INVITE, false)
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> {
                            String currentCustomId = userRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getUserPrefix() + "0000000000");
                            user.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
                            user.setPasswordHash(passwordEncoder.encode(acceptInviteDto.getPassword()));
                            user.setAccountStatus(AccountStatus.PAID_AND_ACTIVE);
                            user.setLastModifiedBy(user.getId());
                            return user;
                        })
                        .map(userRepository::save)
                        .map(user -> {
                            EmployeePreferences preferences = new EmployeePreferences();
                            preferences.setCreatedBy(user.getId());
                            preferences.setUserId(user.getId());
                            employeePreferencesRepository.save(preferences);
                            return user;
                        })
                        .flatMap(user -> userOrganizationRepository.findOneByIdAndDeleteFlag(user.getId(), false))
                        .map(user -> userRepository.findOneByIdAndDeleteFlag(token.getCreatedBy(), false)
                                .map(admin -> {
                                    notificationInternalService.notifyAdminAboutNewUser(user, admin, appUrlConfig.getLoginUrl());
                                    return admin;
                                })
                        )
                )
                .orElseThrow(() -> new EntityNotFoundException("invite key"));
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
     * First part of password reset request
     *
     * @param email user email
     */
    public void requestPasswordReset(String email) {
        userRepository
                .findOneByEmailAndDeleteFlag(email, false)
                .map(user -> {
                    UserTokens token = new UserTokens(UserTokenType.RESET);
                    token.setUserId(user.getId());
                    token.setCreatedBy("SYSTEM");
                    userTokenRepository.save(token);
                    String resetUrl = appUrlConfig.getResetPasswordUrl(token.getToken());
                    // @todo change mail update url
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
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(passwordResetDto.getKey(), UserTokenType.RESET,
                        false)
                .map(token -> {
                    if (token.getCreatedDate().isBefore(Instant.now()
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
    //@TODO update details as per updated dto
    public UserDetailsDto getLoggedInUserAccount() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(UserMapper::entityToDto)
                )
                .orElseThrow(UnauthorizedException::new);
    }

}
