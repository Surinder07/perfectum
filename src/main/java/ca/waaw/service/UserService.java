package ca.waaw.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.config.applicationconfig.AppValidityTimeConfig;
import ca.waaw.domain.Organization;
import ca.waaw.domain.User;
import ca.waaw.dto.*;
import ca.waaw.email.javamailsender.user.UserMailService;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.ExpiredKeyException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
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

    private final AppValidityTimeConfig appValidityTimeConfig;

    private final AppUrlConfig appUrlConfig;

    private final PasswordEncoder passwordEncoder;

    private final UserMailService userMailService;

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
    public void registerUser(RegisterUserDto userDTO) {
        userRepository.findOneByInviteKey(userDTO.getInviteKey())
                .filter(user -> user.getCreatedDate().isAfter(Instant.now()
                        .minus(appValidityTimeConfig.getUserInvite(), ChronoUnit.DAYS)))
                .map(user -> {
                    UserMapper.updateInvitedUser(userDTO, user);
                    user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
                    return user;
                })
                .map(userRepository::save)
                // TODO send notification to admin
                .orElseThrow(() -> new ExpiredKeyException("invite"));
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
        user.setOrganizationId(organization.getId());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));

        organizationRepository.save(organization);
        userRepository.save(user);
        log.info("New Organization created: {}", organization);
        log.info("new User registered: {}", user);
        String activationUrl = appUrlConfig.getActivateAccountUrl(user.getActivationKey());
        userMailService.sendActivationEmail(user, activationUrl);
    }

//
//    /**
//     * Updates the details of current logged-in user
//     *
//     * @param userDto all user details to update
//     */
//    public Mono<String> updateUserDetails(UserUpdateDTO userDto) {
//        return SecurityUtils.getCurrentUserLogin()
//                .switchIfEmpty(Mono.error(Exceptions.EntityNotFoundException("current user login")))
//                .flatMap(userRepository::findOneByLogin)
//                .switchIfEmpty(Mono.error(Exceptions.EntityNotFoundException("user")))
//                .map(user -> {
//                    UserMapper.updateNonNullValuesInDocument(userDto, user);
//                    return user;
//                })
//                .flatMap(userRepository::save)
//                .doOnNext(user -> log.debug("Updated Information for User: {}", user))
//                .thenReturn("");
//    }

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
    public String activateUser(String key) {
        log.debug("Activating user for activation key {}", key);
        userRepository.findOneByActivationKey(key)
                .map(user -> {
                    user.setStatus(EntityStatus.ACTIVE);
                    user.setActivationKey(null);
                    user.setLastModifiedDate(Instant.now());
                    return user;
                })
                .map(userRepository::save)
                .orElseThrow(() -> new ExpiredKeyException("activation"));
        try {
            InputStreamReader stream = new InputStreamReader(new ClassPathResource("templates/mail/SuccessPage.html",
                    this.getClass().getClassLoader()).getInputStream());
            return IOUtils.toString(stream);
        } catch (IOException e) {
            return "<h1>Success</h1>";
        }
    }

    /**
     * First part of password reset request
     *
     * @param email user email
     */
    public void requestPasswordReset(String email) {
        userRepository
                .findOneByEmailAndDeleteFlag(email, false)
                .filter(user -> user.getStatus().equals(EntityStatus.ACTIVE))
                .map(user -> {
                    user.setResetKey(CommonUtils.Random.generateRandomKey());
                    user.setResetDate(Instant.now());
                    user.setLastModifiedBy(user.getId());
                    return user;
                })
                .map(userRepository::save)
                // TODO send email
                .orElseThrow(() -> new EntityNotFoundException("email"));
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
     * Sends an invite to user email
     *
     * @param inviteUserDto new user details
     */
    public void inviteNewUsers(InviteUserDto inviteUserDto) {
        if (!SecurityUtils.isCurrentUserInRole(Authority.ADMIN, Authority.MANAGER)) {
            throw new UnauthorizedException();
        }
        userRepository.findOneByEmailAndDeleteFlag(inviteUserDto.getEmail(), false)
                .ifPresent(user -> {
                    log.debug("Invited email ({}) is already a member", inviteUserDto.getEmail());
                    throw new EntityAlreadyExistsException("email", inviteUserDto.getEmail());
                });
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(inviteUserDto.getEmail());
        user.setFirstName(inviteUserDto.getFirstName());
        user.setLastName(inviteUserDto.getLastName());
        user.setEmployeeId(inviteUserDto.getEmployeeId());
        user.setAuthority(Authority.valueOf(inviteUserDto.getRole()));
        user.setStatus(EntityStatus.PENDING);
        // TODO Add location_id and role_id
        user.setInviteKey(CommonUtils.Random.generateRandomKey());
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .ifPresent(admin -> {
                    user.setLastModifiedBy(admin.getId());
                    user.setCreatedBy(admin.getId());
                    user.setOrganizationId(admin.getOrganizationId());
                });
        userRepository.save(user);
        log.info("New User added to database (pending for accepting invite: {}", user);
        // TODO Send invitation mail
        log.info("Invitation sent to the user");
    }

    /**
     * @return User details of the logged-in user account
     */
    public UserDetailsDto getLoggedInUserAccount() {
        return SecurityUtils.getCurrentUserLogin()
                .map(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(UserMapper::entityToDto).orElseThrow(EntityNotFoundException::new))
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
                        throw new EntityAlreadyExistsException("username", userDTO.getUsername());
                    }
                });
    }

}
