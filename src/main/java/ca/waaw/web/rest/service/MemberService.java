package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.user.EmployeePreferences;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.domain.user.UserTokens;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.appnotifications.InviteUserMailDto;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.shifts.ShiftSchedulingPreferences;
import ca.waaw.dto.userdtos.EmployeePreferencesDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.dto.userdtos.UpdateUserDto;
import ca.waaw.dto.userdtos.UserDetailsForAdminDto;
import ca.waaw.enumration.NotificationType;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.user.UserTokenType;
import ca.waaw.filehandler.FileHandler;
import ca.waaw.filehandler.enumration.PojoToMap;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.user.EmployeePreferencesRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserTokenRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.AppNotificationService;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.errors.exceptions.application.MissingRequiredFieldsException;
import ca.waaw.web.rest.utils.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MemberService {

    private final Logger log = LogManager.getLogger(MemberService.class);

    private final UserRepository userRepository;

    private final UserTokenRepository userTokenRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final AppUrlConfig appUrlConfig;

    private final UserMailService userMailService;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final EmployeePreferencesRepository employeePreferencesRepository;

    private final FileHandler fileHandler;

    private final ShiftsRepository shiftsRepository;

    private final AppNotificationService appNotificationService;

    private final MessageSource messageSource;

    /**
     * Sends an invitation to user email
     *
     * @param inviteUserDto new user details
     */
    @Transactional(rollbackFor = Exception.class)
    public void inviteNewUsers(InviteUserDto inviteUserDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(UnauthorizedException::new);
        locationRoleRepository.findOneByIdAndDeleteFlag(inviteUserDto.getLocationRoleId(), false)
                .ifPresent(role -> inviteUserDto.setAuthority(role.isAdminRights() ? Authority.MANAGER : Authority.EMPLOYEE));
        inviteNewUsers(Collections.singletonList(inviteUserDto), true, admin);
    }

    /**
     * Resend invite to a user who has not accepted it yet
     *
     * @param userId User to which invitation is to be resent
     */
    public void resendInvite(String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> userRepository.findOneByIdAndDeleteFlag(userId, false)
                        .map(user -> {
                            if ((loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                    !user.getLocationId().equals(loggedUser.getLocationId())) ||
                                    !user.getOrganizationId().equals(loggedUser.getOrganizationId())) {
                                return null;
                            }
                            if (user.getAccountStatus().equals(AccountStatus.INVITED)) {
                                userTokenRepository.findOneByUserIdAndTokenTypeOrderByCreatedDateDesc(userId, UserTokenType.INVITE)
                                        .stream().findFirst()
                                        .map(token -> {
                                            token.setExpired(true);
                                            return userTokenRepository.save(token);
                                        })
                                        .map(token -> {
                                            UserTokens newToken = new UserTokens(UserTokenType.INVITE);
                                            newToken.setUserId(token.getUserId());
                                            newToken.setCreatedBy(loggedUser.getId());
                                            return newToken;
                                        })
                                        .map(userTokenRepository::save)
                                        .map(newToken -> {
                                            InviteUserMailDto mailDto = new InviteUserMailDto();
                                            mailDto.setUser(user);
                                            mailDto.setInviteUrl(appUrlConfig.getInviteUserUrl(newToken.getToken()));
                                            userMailService.sendInvitationEmail(Collections.singletonList(mailDto),
                                                    loggedUser.getOrganization().getName());
                                            return newToken;
                                        });
                                return user;
                            }
                            return null;
                        })
                        .orElseThrow(() -> new EntityNotFoundException("user"))
                );
    }

    /**
     * @param file Multipart file containing new users information
     * @return Generic message
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponseMessageDto inviteNewUsersByUpload(MultipartFile file) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        // Converting file to Input Stream so that it is available in the async process below
        InputStream fileInputStream;
        String fileName;
        try {
            fileInputStream = file.getInputStream();
            fileName = file.getOriginalFilename();
        } catch (IOException e) {
            log.error("Exception while reading file.", e);
            throw new FileNotReadableException();
        }
        UserOrganization admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(UnauthorizedException::new);
        String role = admin.getAuthority().toString();

        Set<String> missingFields = new HashSet<>();
        /*
         * Location and locationId here contains names coming from excel/csv sheet, we will fetch Ids from
         * database and replace them.
         */
        List<InviteUserDto> inviteUserDtoList = fileHandler.readExcelOrCsv(fileInputStream, fileName,
                InviteUserDto.class, missingFields, PojoToMap.INVITE_USERS);
        List<Location> locations = locationRepository.getListByNameAndOrganization(inviteUserDtoList
                        .stream().map(InviteUserDto::getLocationId).map(String::toLowerCase).collect(Collectors.toList()),
                admin.getOrganizationId());
        List<LocationRole> locationRoles = locationRoleRepository.getListByNameAndLocation(inviteUserDtoList
                        .stream().map(InviteUserDto::getLocationRoleId).map(String::toLowerCase).collect(Collectors.toList()),
                locations.stream().map(Location::getId).collect(Collectors.toList()));
        inviteUserDtoList.forEach(inviteUserDto -> {
            if (StringUtils.isEmpty(inviteUserDto.getLocationId()) && role.equals("ADMIN")) {
                missingFields.add("location");
                inviteUserDtoList.remove(inviteUserDto);
            } else {
                Location location = role.equals("ADMIN") ? locations.stream().filter(loc -> loc.getName()
                                .equalsIgnoreCase(inviteUserDto.getLocationId())).findFirst()
                        .orElse(null) : admin.getLocation();
                LocationRole locationRole = locationRoles.stream().filter(locRole -> locRole.getName()
                                .equalsIgnoreCase(inviteUserDto.getLocationRoleId())).findFirst()
                        .orElse(null);
                if (location == null)
                    throw new EntityNotFoundException("location", inviteUserDto.getLocationId());
                if (locationRole == null)
                    throw new EntityNotFoundException("locationRole", inviteUserDto.getLocationRoleId());
                inviteUserDto.setLocationId(location.getId());
                inviteUserDto.setLocationRoleId(locationRole.getId());
                inviteUserDto.setAuthority(locationRole.isAdminRights() ? Authority.MANAGER : Authority.EMPLOYEE);
            }
        });
        if (missingFields.size() > 0) {
            throw new MissingRequiredFieldsException("excel/csv", missingFields.toArray(missingFields.toArray(new String[0])));
        }
        CompletableFuture.runAsync(() -> {
            try {
                inviteNewUsers(inviteUserDtoList, false, admin);
            } catch (Exception e) {
                e.printStackTrace();
            }
            NotificationInfoDto notificationInfo = NotificationInfoDto.builder()
                    .receiverUuid(admin.getId())
                    .receiverName(admin.getFullName())
                    .receiverMail(admin.getEmail())
                    .receiverUsername(admin.getUsername())
                    .receiverMobile(admin.getMobile() == null ? null : admin.getCountryCode() + admin.getMobile())
                    .language(admin.getLangKey() == null ? null : admin.getLangKey())
                    .type(NotificationType.EMPLOYEE)
                    .build();
            DateTimeDto now = DateAndTimeUtils.getCurrentDateTime(admin.getAuthority().equals(Authority.ADMIN) ?
                    admin.getOrganization().getTimezone() : admin.getLocation().getTimezone());
            appNotificationService.sendApplicationNotification(MessageConstants.usersUpload, notificationInfo, false,
                    now.getDate(), now.getTime());
        });
        return new ApiResponseMessageDto(messageSource.getMessage(ApiResponseMessageKeys.uploadNewEmployees,
                null, new Locale(admin.getLangKey())));
    }

    /**
     * @return all Employees and Admins under logged-in user
     */
    public PaginationDto getAllUsers(int pageNo, int pageSize, String searchKey, String locationId, String roleId,
                                     String type, String status) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("firstName", "lastName").ascending());
        AtomicReference<String> timezone = new AtomicReference<>(null);
        Page<UserOrganization> userPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    timezone.set(admin.getAuthority().equals(Authority.ADMIN) ? admin.getOrganization().getTimezone() :
                            admin.getLocation().getTimezone());
                    Boolean isFullTime = null;
                    AccountStatus accountStatus = null;
                    if (type != null) {
                        isFullTime = type.equalsIgnoreCase("full_time");
                    }
                    if (status != null) {
                        accountStatus = AccountStatus.valueOf(status.toUpperCase(Locale.ROOT));
                    }
                    String finalLocationId = admin.getAuthority().equals(Authority.MANAGER) ? admin.getLocationId() : locationId;
                    return userOrganizationRepository.searchAndFilterUsers(searchKey, admin.getOrganizationId(), finalLocationId,
                            roleId, isFullTime, accountStatus, admin.getAuthority().equals(Authority.MANAGER), pageable);
                })
                .orElseThrow(UnauthorizedException::new);
        return CommonUtils.getPaginationResponse(userPage, UserMapper::entityToUserDetailsForListing, timezone.get());
    }

    /**
     * @param userId id for which user info is required
     * @return User details for that user
     */
    public UserDetailsForAdminDto getMemberById(String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .map(admin -> userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                                .map(user -> {
                                    if ((!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                            (admin.getAuthority().equals(Authority.MANAGER) &&
                                                    !admin.getLocationId().equals(user.getLocationId())))) {
                                        return null;
                                    }
                                    return employeePreferencesRepository.findOneByUserIdAndIsExpired(user.getId(), false)
                                            .map(preference -> UserMapper.entityToUserDetailsForAdmin(user, preference))
                                            .orElse(UserMapper.entityToUserDetailsForAdmin(user, null));
                                })
                                .map(user -> {
                                    user.setImageUrl(user.getImageUrl() == null ? null : appUrlConfig.getImageUrl(user.getId(), "profile"));
                                    return user;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("user")))
                )
                .orElseThrow(() -> new EntityNotFoundException("user"));
    }

    /**
     * @param employeePreferencesDto all employee preferences to be updated
     */
    @Transactional(rollbackFor = Exception.class)
    public void addEmployeePreferences(EmployeePreferencesDto employeePreferencesDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        List<EmployeePreferences> preferencesToSave = new ArrayList<>();
        AtomicReference<ShiftSchedulingPreferences> shiftSchedulingPreferences = new AtomicReference<>();
        String adminUserId = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> Optional.of(userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .flatMap(admin -> userOrganizationRepository.findOneByIdAndDeleteFlag(employeePreferencesDto.getUserId(), false)
                                .map(user -> {
                                    if ((!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                            (admin.getAuthority().equals(Authority.MANAGER) &&
                                                    !admin.getLocationId().equals(user.getLocationId())))) {
                                        throw new UnauthorizedException();
                                    }
                                    return user.getLocationRole();
                                })
                                .map(locationRole -> {
                                    // These preferences will be used to validate Employee preferences
                                    shiftSchedulingPreferences.set(ShiftSchedulingUtils.preferenceMappingFunction(locationRole));
                                    return admin;
                                })
                        )
                        .orElseThrow(() -> new EntityNotFoundException("user")))
                )
                .flatMap(admin -> employeePreferencesRepository.findOneByUserIdAndIsExpired(employeePreferencesDto.getUserId(), false)
                        .map(preference -> {
                            preference.setExpired(true);
                            preferencesToSave.add(preference);
                            return admin.getId();
                        })
                )
                .orElseThrow(AuthenticationException::new);
        EmployeePreferences newPreference = UserMapper.employeePreferencesToEntity(employeePreferencesDto);
        newPreference.setCreatedBy(adminUserId);
        preferencesToSave.add(newPreference);
        employeePreferencesRepository.saveAll(preferencesToSave);
        if (ShiftSchedulingUtils.validateEmployeePreference(shiftSchedulingPreferences.get(), employeePreferencesDto)) {
            // TODO notify admin about preference mismatch
        }
        log.info("New preference saved for the employee: {}", newPreference);
    }

    public void updateMember(UpdateUserDto memberDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> userOrganizationRepository.findOneByIdAndDeleteFlag(memberDto.getId(), false)
                        .map(user -> {
                            validateDependenciesIfLocationOrRoleUpdate(memberDto, user);
                            if (!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    (admin.getAuthority().equals(Authority.MANAGER) &&
                                            !user.getLocationId().equals(admin.getLocationId()))) {
                                return null;
                            }
                            if (admin.getAuthority().equals(Authority.MANAGER))
                                memberDto.setLocationId(user.getLocationId());
                            if (memberDto.getLocationId() != null && !user.getLocationId().equals(memberDto.getLocationId())) {
                                locationRepository.findOneByIdAndDeleteFlag(memberDto.getLocationId(), false)
                                        .ifPresent(location -> {
                                            if (!location.getOrganizationId().equals(admin.getOrganizationId())) {
                                                throw new EntityNotFoundException("location");
                                            }
                                        });
                            }
                            if (memberDto.getRoleId() != null && !user.getLocationRoleId().equals(memberDto.getRoleId())) {
                                locationRoleRepository.findOneByIdAndDeleteFlag(memberDto.getRoleId(), false)
                                        .ifPresent(role -> {
                                            if (!role.getOrganizationId().equals(admin.getOrganizationId()) ||
                                                    (admin.getAuthority().equals(Authority.MANAGER) &&
                                                            !admin.getLocationId().equals(role.getLocationId()))
                                            ) {
                                                throw new EntityNotFoundException("role");
                                            } else
                                                user.setAuthority(role.isAdminRights() ? Authority.MANAGER : Authority.EMPLOYEE);
                                        });
                            }
                            UserMapper.updateUserDtoToEntity(memberDto, user);
                            user.setLastModifiedBy(admin.getId());
                            return user;
                        })
                        .map(userOrganizationRepository::save)
                )
                .orElseThrow(() -> new EntityNotFoundException("user"));
        // TODO Send notification to employee
    }

    /**
     * @param id id for which user is to be deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(String id) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(admin -> userRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(user -> {
                            if (!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    (admin.getAuthority().equals(Authority.MANAGER) &&
                                            !admin.getLocationId().equals(user.getLocationId()))) {
                                return null;
                            }
                            int shifts = shiftsRepository.findAllByUserIdAndStartAfterAndDeleteFlag(user.getId(), Instant.now(), false).size();
                            if (shifts > 0) throw new EntityNotDeletableException("user", "assigned shifts");
                            user.setDeleteFlag(true);
                            user.setLastModifiedBy(admin.getId());
                            userTokenRepository.findOneByUserIdAndTokenTypeAndIsExpired(user.getId(), UserTokenType.INVITE, false)
                                    .map(token -> {
                                        token.setExpired(true);
                                        return userTokenRepository.save(token);
                                    });
                            return user;
                        })
                )
                .map(userRepository::save)
                .orElseThrow(() -> new EntityNotFoundException("user"));
    }

    /**
     * @param id id for which user's status is to be changed
     */
    public void toggleActiveMember(String id) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(admin -> userRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(user -> {
                            if (!user.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    (admin.getAuthority().equals(Authority.MANAGER) &&
                                            !admin.getLocationId().equals(user.getLocationId()))) {
                                return null;
                            }
                            if (user.getAccountStatus().equals(AccountStatus.DISABLED)) {
                                boolean activeParent = locationRoleRepository.findOneByIdAndDeleteFlag(user.getLocationRoleId(), false)
                                        .filter(LocationRole::isActive)
                                        .isPresent();
                                if (!activeParent) throw new EntityCantBeActivatedException("employee", "role");
                            } else {
                                int shifts = shiftsRepository.findAllByUserIdAndStartAfterAndDeleteFlag(user.getId(), Instant.now(), false).size();
                                if (shifts > 0) throw new EntityNotDeletableException("user", "assigned shifts");
                            }
                            user.setAccountStatus(user.getAccountStatus().equals(AccountStatus.DISABLED) ?
                                    AccountStatus.PAID_AND_ACTIVE : AccountStatus.DISABLED);
                            user.setLastModifiedBy(admin.getId());
                            return user;
                        })
                )
                .map(userRepository::save)
                .orElseThrow(() -> new EntityNotFoundException("user"));
    }

    /**
     * Sends an invitation to user email
     *
     * @param inviteUserDtoList new user details list
     * @param throwError        If an email already exist, should throw error?
     * @param admin             {@link UserOrganization} object containing admin info
     */
    private void inviteNewUsers(List<InviteUserDto> inviteUserDtoList, boolean throwError, UserOrganization admin) {
        List<String> emails = inviteUserDtoList.stream().map(InviteUserDto::getEmail).collect(Collectors.toList());
        List<String> emailAlreadyExist = userRepository.findAllByEmailInAndDeleteFlag(emails, false)
                .stream().map(User::getEmail).map(String::toLowerCase).collect(Collectors.toList());
        if (emailAlreadyExist.size() > 0 && throwError)
            throw new EntityAlreadyExistsException("email", "address", emails.get(0));
        List<UserTokens> tokenList = new ArrayList<>();
        List<InviteUserMailDto> mailDtoList = inviteUserDtoList.stream()
                .filter(user -> !emailAlreadyExist.contains(user.getEmail()))
                .map(UserMapper::inviteUserDtoToEntity)
                .map(user -> {
                    user.setCreatedBy(admin.getId());
                    user.setOrganizationId(admin.getOrganization().getId());
                    if (admin.getAuthority().equals(Authority.MANAGER)) {
                        user.setLocationId(admin.getLocationId());
                    }
                    UserTokens token = new UserTokens(UserTokenType.INVITE);
                    token.setUserId(user.getId());
                    token.setCreatedBy(admin.getId());
                    tokenList.add(token);
                    InviteUserMailDto mailDto = new InviteUserMailDto();
                    mailDto.setUser(user);
                    mailDto.setInviteUrl(appUrlConfig.getInviteUserUrl(token.getToken()));
                    return mailDto;
                })
                .collect(Collectors.toList());
        List<User> users = mailDtoList.stream().map(InviteUserMailDto::getUser).collect(Collectors.toList());
        String organizationName = admin.getOrganization().getName();
        userRepository.saveAll(users);
        userTokenRepository.saveAll(tokenList);
        log.info("New User(s) added to database (pending for accepting invite): {}", users);
        CompletableFuture.runAsync(() -> {
            userMailService.sendInvitationEmail(mailDtoList, organizationName);
            log.info("Invitation successfully sent to the user(s)");
        });
    }

    private void validateDependenciesIfLocationOrRoleUpdate(UpdateUserDto memberDto, UserOrganization user) {
        if (!(memberDto.getRoleId().equals(user.getLocationRoleId()) &&
                memberDto.getLocationId().equals(user.getLocationId()))) {
            int shifts = shiftsRepository.findAllByUserIdAndStartAfterAndDeleteFlag(user.getId(), Instant.now(), false).size();
            if (shifts > 0)
                throw new BadRequestException("Please remove all dependencies before updating location or role for user.");
        }
    }

}