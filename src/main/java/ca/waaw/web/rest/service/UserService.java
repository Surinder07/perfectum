package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.Organization;
import ca.waaw.domain.PromotionCode;
import ca.waaw.domain.User;
import ca.waaw.domain.UserTokens;
import ca.waaw.dto.userdtos.*;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.PromoCodeType;
import ca.waaw.enumration.UserToken;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
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

    @Transactional(rollbackFor = Exception.class)
    public void registerNewUser(NewRegistrationDto registrationDto) {
        userRepository.findOneByEmailAndDeleteFlag(registrationDto.getEmail(), false)
                .ifPresent(user -> {
                    if (user.getAccountStatus().equals(AccountStatus.EMAIL_PENDING))
                        userRepository.delete(user);
                    else
                        throw new EntityAlreadyExistsException("email", registrationDto.getEmail());
                });
        String currentCustomId = userRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getUserPrefix() + "0000000000");
        User user = UserMapper.registerDtoToUserEntity(registrationDto);
        user.setWaawId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        UserTokens token = new UserTokens(UserToken.ACTIVATION);
        token.setUserId(user.getId());
        token.setCreatedBy("SYSTEM");
        userRepository.save(user);
        userTokenRepository.save(token);
        log.info("New User registered: {}", user);
        log.info("New activation token generated: {}", token);
        String activationUrl = appUrlConfig.getActivateAccountUrl(token.getToken());
        userMailService.sendActivationEmail(user, activationUrl);
    }

    /**
     * Used to activate a user by verifying email when they click link in their mails.
     *
     * @param verificationKey activation verificationKey received in mail
     */
    public UserDetailsNewDto verifyEmail(String verificationKey) {
        log.info("Activating user for verificationKey {}", verificationKey);
        return userTokenRepository
                .findOneByTokenAndTokenTypeAndIsExpired(verificationKey, UserToken.ACTIVATION, false)
                .flatMap(userTokens -> {
                    if (userTokens.getCreatedDate().isAfter(Instant.now()
                            .minus(appValidityTimeConfig.getActivationLink(), ChronoUnit.DAYS))) {
                        userTokens.setExpired(true);
                        userTokenRepository.save(userTokens);
                        throw new ExpiredKeyException("verification");
                    } else {
                        return userRepository.findOneByIdAndDeleteFlag(userTokens.getUserId(), false)
                                .map(user -> {
                                    user.setAccountStatus(AccountStatus.PROFILE_PENDING);
                                    user.setLastModifiedDate(Instant.now());
                                    return user;
                                })
                                .map(userRepository::save)
                                .map(UserMapper::entityToDetailsDto);
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
    public void completeRegistration(CompleteRegistrationDto completeRegistrationDto) {
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(completeRegistrationDto.getVerificationKey(),
                        UserToken.ACTIVATION, true)
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> {
                            Organization organization = UserMapper.completeRegistrationToEntity(completeRegistrationDto, user);
                            String currentOrgCustomId = organizationRepository.getLastUsedCustomId()
                                    .orElse(appCustomIdConfig.getOrganizationPrefix() + "0000000000");
                            int trialDays = 0;
                            if (StringUtils.isNotEmpty(completeRegistrationDto.getPromoCode())) {
                                trialDays = promotionCodeRepository.findOneByCodeAndTypeAndDeleteFlag(completeRegistrationDto.getPromoCode(),
                                                PromoCodeType.TRIAL, false)
                                        .map(PromotionCode::getPromotionValue)
                                        .orElseThrow(() -> new EntityNotFoundException("promo code"));
                            }
                            organization.setTrialDays(trialDays);
                            organization.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
                            organizationRepository.save(organization);
                            return user;
                        })
                        .map(userRepository::save)
                )
                .orElseThrow(() -> new EntityNotFoundException("verification key"));
    }

    public Map<String, Object> validatePromoCode(String promoCode) {
        return promotionCodeRepository.findOneByCodeAndDeleteFlag(promoCode, false)
                .map(code -> {
                    if (code.getExpiryDate().isBefore(Instant.now())) {
                        return null;
                    }
                    Map<String, Object> response = new HashMap<>();
                    response.put("codeType", code.getType().name());
                    response.put("codeValue", code.getPromotionValue());
                    return response;
                })
                .orElseThrow(() -> new EntityNotFoundException("promotion code"));
    }

    /**
     * If a user accepts invite, this method will fetch their details
     *
     * @param invitationKey invite key received in mail
     */
    public UserDetailsNewDto checkInviteLink(String invitationKey) {
        log.info("Getting details for user with invitation key: {}", invitationKey);
        return userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(invitationKey, UserToken.INVITE, false)
                .map(token -> {
                    if (token.getCreatedDate().isAfter(Instant.now()
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
    public void acceptInvite(AcceptInviteDto acceptInviteDto) {
        userTokenRepository.findOneByTokenAndTokenTypeAndIsExpired(acceptInviteDto.getInviteKey(), UserToken.INVITE, false)
                .flatMap(token -> userRepository.findOneByIdAndDeleteFlag(token.getUserId(), false)
                        .map(user -> {
                            user.setPasswordHash(passwordEncoder.encode(acceptInviteDto.getPassword()));
                            return user;
                        })
                        .map(userRepository::save)
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
     * First part of password reset request
     *
     * @param email user email
     */
    public void requestPasswordReset(String email) {
        userRepository
                .findOneByEmailAndDeleteFlag(email, false)
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

}
