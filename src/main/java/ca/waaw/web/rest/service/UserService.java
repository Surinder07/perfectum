package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppRegexConfig;
import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.payments.PromotionCode;
import ca.waaw.domain.user.EmployeePreferences;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.domain.user.UserTokens;
import ca.waaw.dto.payments.NewPaymentDto;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.Currency;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.FileType;
import ca.waaw.enumration.payment.PromoCodeType;
import ca.waaw.enumration.payment.TransactionType;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.user.UserTokenType;
import ca.waaw.mapper.UserMapper;
import ca.waaw.payment.stripe.StripeService;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.payments.PromotionCodeRepository;
import ca.waaw.repository.user.EmployeePreferencesRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserTokenRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.service.AppNotificationService;
import ca.waaw.service.UserMailService;
import ca.waaw.service.WebSocketService;
import ca.waaw.storage.AzureStorage;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.errors.exceptions.application.StripeCustomerException;
import ca.waaw.web.rest.utils.CommonUtils;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

    private final PaymentsService paymentsService;

    private final PasswordEncoder passwordEncoder;

    private final UserMailService userMailService;

    private final AppNotificationService appNotificationService;

    private final AzureStorage azureStorage;

    private final WebSocketService webSocketService;

    private final StripeService stripeService;

    private final AppValidityTimeConfig appValidityTimeConfig;

    private final AppUrlConfig appUrlConfig;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppRegexConfig appRegexConfig;

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
                            userRepository.findOneByUsernameAndDeleteFlag(completeRegistrationDto.getUsername(), false)
                                    .ifPresent(user1 -> {
                                        throw new EntityAlreadyExistsException("user", "username", completeRegistrationDto.getUsername());
                                    });
                            if (user.getAuthority().equals(Authority.ADMIN)) {
                                if (StringUtils.isEmpty(completeRegistrationDto.getOrganizationName())) {
                                    throw new BadRequestException("Organization Name is required.", "Organization.name");
                                }
                                String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                                        .orElse(appCustomIdConfig.getOrganizationPrefix() + "0000000000");
                                Organization organization = new Organization();
                                organization.setName(completeRegistrationDto.getOrganizationName());
                                organization.setTimezone(completeRegistrationDto.getTimezone());
                                if (StringUtils.isNotEmpty(completeRegistrationDto.getFirstDayOfWeek())) {
                                    organization.setFirstDayOfWeek(DaysOfWeek.valueOf(completeRegistrationDto.getFirstDayOfWeek()));
                                }
                                int trialDays = 0;
                                if (StringUtils.isNotEmpty(completeRegistrationDto.getPromoCode())) {
                                    trialDays = promotionCodeRepository.findOneByCodeAndTypeAndDeleteFlag(completeRegistrationDto.getPromoCode(),
                                                    PromoCodeType.TRIAL, false)
                                            .map(PromotionCode::getPromotionValue)
                                            .orElseThrow(() -> new EntityNotFoundException("promo code"));
                                    organization.setTrialEndDate(Instant.now().plus(trialDays, ChronoUnit.DAYS));
                                } else {
                                    organization.setTrialEndDate(Instant.now());
                                    organization.setPaymentPending(true);
                                }
                                if (trialDays == 0) {
                                    user.setAccountStatus(AccountStatus.PAYMENT_PENDING);
                                    organization.setPaymentPending(true);
                                } else user.setAccountStatus(AccountStatus.PAYMENT_INFO_PENDING);
                                organization.setCreatedBy(user.getId());
                                organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
                                organizationRepository.save(organization);
                                if (trialDays == 0) {
                                    NewPaymentDto platformInvoice = NewPaymentDto.builder()
                                            .paymentDate(organization.getTrialEndDate())
                                            .currency(Currency.CAD)
                                            .organizationId(organization.getId())
                                            .quantity(1)
                                            .unitPrice(200)
                                            .type(TransactionType.PLATFORM_FEE)
                                            .totalAmount(200)
                                            .build();
                                    paymentsService.createNewInvoice(platformInvoice);
                                }
                                user.setOrganizationId(organization.getId());
                                addCustomerOnStripe(user, organization.getName());
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

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto completePaymentInfo(String stripeTokenId) {
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(user -> {
                            try {
                                stripeService.createNewCard(stripeTokenId, user.getStripeId());
                            } catch (StripeException e) {
                                e.printStackTrace();
                                throw new StripeCustomerException();
                            }
                            if (user.getOrganization().getTrialEndDate() != null && user.getOrganization().getTrialEndDate().isBefore(Instant.now())) {
                                user.setAccountStatus(AccountStatus.PAYMENT_PENDING);
                            } else {
                                user.setAccountStatus(AccountStatus.TRIAL_PERIOD);
                            }
                            NewPaymentDto platformInvoice = NewPaymentDto.builder()
                                    .paymentDate(user.getOrganization().getTrialEndDate())
                                    .currency(Currency.CAD)
                                    .organizationId(user.getOrganizationId())
                                    .quantity(1)
                                    .unitPrice(200)
                                    .type(TransactionType.PLATFORM_FEE)
                                    .totalAmount(200)
                                    .build();
                            paymentsService.createNewInvoice(platformInvoice);
                            user.setLastModifiedBy(user.getId());
                            return user;
                        })
                        .map(userOrganizationRepository::save)
                )
                .orElseThrow(AuthenticationException::new);
        // Updating jwt with the new status
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
                        response.put("message", String.format("Trial Period for %s days has been added to your account.", code.getPromotionValue()));
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
                                    appNotificationService.notifyAdminAboutNewUser(user, admin, appUrlConfig.getLoginUrl());
                                    return admin;
                                })
                        )
                        .map(user -> {
                            token.setExpired(true);
                            userTokenRepository.save(token);
                            return user;
                        })
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
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> {
                            token.setExpired(true);
                            userTokenRepository.save(token);
                            return user;
                        }))
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
                        .map(user -> {
                            user.setImageUrl(user.getImageUrl() == null ? null : appUrlConfig.getImageUrl(user.getId(), "profile"));
                            user.setOrganizationLogoUrl(user.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(user.getOrganizationId(), "organization"));
                            return user;
                        })
                )
                .orElseThrow(UnauthorizedException::new);
    }

    /**
     * Update logged user details (Available only for ADMIN
     *
     * @param updateLoggedUserDto details to update
     */
    public void updateLoggedUserDetails(UpdateLoggedUserDto updateLoggedUserDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        String userId = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    loggedUser.setFirstName(updateLoggedUserDto.getFirstName());
                    loggedUser.setLastName(updateLoggedUserDto.getLastName());
                    loggedUser.setMobile(String.valueOf(updateLoggedUserDto.getMobile()));
                    loggedUser.setCountry(updateLoggedUserDto.getCountry());
                    loggedUser.setCountryCode(updateLoggedUserDto.getCountryCode());
                    loggedUser.setLastModifiedBy(loggedUser.getId());
                    return loggedUser;
                })
                .map(userRepository::save)
                .map(user -> CommonUtils.logMessageAndReturnObject(user, "info", UserService.class,
                        "Updated logged in user details: {}", user))
                .map(User::getId)
                .orElseThrow(AuthenticationException::new);
        UserDetailsDto response = userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                .map(UserMapper::entityToDto)
                .map(user -> {
                    user.setImageUrl(user.getImageUrl() == null ? null : appUrlConfig.getImageUrl(user.getId(), "profile"));
                    user.setOrganizationLogoUrl(user.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(user.getOrganizationId(), "organization"));
                    return user;
                })
                .orElseThrow(AuthenticationException::new);
        webSocketService.updateUserDetailsForUi(response.getUsername(), response);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEmailInit(String email) {
        CommonUtils.validateRegexString(appRegexConfig.getEmail(), email, "email");
        AtomicReference<String> verificationUrl = new AtomicReference<>(null);
        User loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    userRepository.findOneByEmailAndDeleteFlag(email.toLowerCase(), false)
                            .ifPresent(checkUser -> {
                                log.error("Email({}) already exits and cannot be added to user, {}", email, user.getEmail());
                                throw new EntityAlreadyExistsException("user", "email", email);
                            });
                    user.setEmailToUpdate(email.toLowerCase());
                    user.setLastModifiedBy(user.getId());
                    return user;
                })
                .map(user -> {
                    UserTokens token = new UserTokens(UserTokenType.UPDATE_EMAIL);
                    token.setUserId(user.getId());
                    token.setCreatedBy(user.getId());
                    verificationUrl.set(appUrlConfig.getUpdateEmailUrl(token.getToken()));
                    userTokenRepository.save(token);
                    return user;
                })
                .map(userRepository::save)
                .map(user -> CommonUtils.logMessageAndReturnObject(user, "info", UserService.class,
                        "Initialized email update process for: {}", user))
                .orElseThrow(AuthenticationException::new);
        userMailService.sendUpdateEmailMail(loggedUser, verificationUrl.get());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEmailFinish(String token) {
        UserOrganization updateUser = userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(token, UserTokenType.UPDATE_EMAIL, false)
                .map(userToken -> {
                    if (userToken.getCreatedDate().isBefore(Instant.now().minus(appValidityTimeConfig.getEmailUpdate(),
                            ChronoUnit.DAYS))) {
                        userToken.setExpired(true);
                        userTokenRepository.save(userToken);
                        throw new ExpiredKeyException("email update");
                    } else return userToken;
                })
                .flatMap(userToken -> userOrganizationRepository.findOneByIdAndDeleteFlag(userToken.getUserId(), false)
                        .map(user -> {
                            user.setEmail(user.getEmailToUpdate());
                            user.setEmailToUpdate(null);
                            user.setLastModifiedBy(user.getId());
                            return user;
                        })
                        .map(userOrganizationRepository::save)
                        .map(user -> {
                            userToken.setExpired(true);
                            userTokenRepository.save(userToken);
                            return user;
                        })
                        .map(user -> {
                            userToken.setExpired(true);
                            userTokenRepository.save(userToken);
                            return user;
                        })
                )
                .orElseThrow(() -> new ExpiredKeyException("email update"));
        UserDetailsDto response = Optional.of(updateUser)
                .map(UserMapper::entityToDto)
                .map(user -> {
                    user.setImageUrl(user.getImageUrl() == null ? null : appUrlConfig.getImageUrl(user.getId(), "profile"));
                    user.setOrganizationLogoUrl(user.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(user.getOrganizationId(), "organization"));
                    return user;
                })
                .orElseThrow(AuthenticationException::new);
        webSocketService.updateUserDetailsForUi(response.getUsername(), response);
    }

    public void updateProfilePic(MultipartFile file) throws Exception {
        String fileName;
        try {
            fileName = azureStorage.uploadFile(file, FileType.PICTURES);
        } catch (IOException e) {
            throw new Exception("There was an error while uploading your image.");
        }
        String userId = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    loggedUser.setImageFile(fileName);
                    loggedUser.setLastModifiedBy(loggedUser.getId());
                    return loggedUser;
                })
                .map(userRepository::save)
                .map(User::getId)
                .orElseThrow(AuthenticationException::new);
        UserDetailsDto response = userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                .map(UserMapper::entityToDto)
                .map(user -> {
                    user.setImageUrl(user.getImageUrl() == null ? null : appUrlConfig.getImageUrl(user.getId(), "profile"));
                    user.setOrganizationLogoUrl(user.getOrganizationLogoUrl() == null ? null : appUrlConfig.getImageUrl(user.getOrganizationId(), "organization"));
                    return user;
                })
                .orElseThrow(AuthenticationException::new);
        webSocketService.updateUserDetailsForUi(response.getUsername(), response);
    }

    public void addCustomerOnStripe(User user, String organizationName) {
        String stripeCustomerId;
        try {
            stripeCustomerId = stripeService.createNewCustomer(user.getEmail(), organizationName,
                    user.getMobile() != null ? user.getCountryCode() + user.getMobile() : null);
        } catch (StripeException e) {
            e.printStackTrace();
            throw new StripeCustomerException();
        }
        user.setStripeId(stripeCustomerId);
    }

}
