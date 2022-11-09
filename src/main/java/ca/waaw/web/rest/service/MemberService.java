package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.*;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.ApiResponseMessageDto;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.ShiftSchedulingPreferences;
import ca.waaw.dto.emailmessagedtos.InviteUserMailDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.dto.userdtos.UserDetailsForAdminDto;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.UserToken;
import ca.waaw.filehandler.FileHandler;
import ca.waaw.filehandler.enumration.PojoToMap;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.errors.exceptions.application.MissingRequiredFieldsException;
import ca.waaw.web.rest.utils.ApiResponseMessageKeys;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.ShiftSchedulingUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
        inviteNewUsers(Collections.singletonList(inviteUserDto), true, admin);
    }

    /**
     * @param userId User to which invitation is to be resent
     */
    public void resendInvite(String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> userRepository.findOneByIdAndDeleteFlag(userId, false)
                        .map(user -> {
                            if (!user.getId().equals(loggedUser.getId()) ||
                                    (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                            !user.getLocationId().equals(loggedUser.getLocationId()))) {
                                return null;
                            }
                            if (user.getAccountStatus().equals(AccountStatus.INVITED)) {
                                userTokenRepository.findOneByUserIdAndTokenType(userId, UserToken.INVITE)
                                        .map(token -> {
                                            token.setExpired(true);
                                            return userTokenRepository.save(token);
                                        })
                                        .map(token -> {
                                            UserTokens newToken = new UserTokens(UserToken.INVITE);
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
                            return null; // TODO throw user already active error
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
        String role = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ? "ADMIN" : "MANAGER";

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
                inviteUserDto.setLocationRoleId(locationRole.getLocationId());
            }
        });
        if (missingFields.size() > 0) {
            throw new MissingRequiredFieldsException("excel/csv", missingFields.toArray(missingFields.toArray(new String[0])));
        }
        CompletableFuture.runAsync(() -> {
            inviteNewUsers(inviteUserDtoList, false, admin);
            // TODO send notification
        });
        return new ApiResponseMessageDto(CommonUtils.getPropertyFromMessagesResourceBundle(ApiResponseMessageKeys
                .uploadNewEmployees, new Locale(admin.getLangKey())));

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
                    if (user.getAuthority().equals(Authority.ADMIN)) {
                        return locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                                .map(location -> {
                                    if (location.getOrganizationId().equals(user.getOrganizationId())) return user;
                                    return null;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("location"));
                    }
                    return user;
                })
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
        return CommonUtils.getPaginationResponse(userPage, UserMapper::entityToUserDetailsForAdmin);
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
                .filter(user -> user.getOrganizationId().equals(locationRole.getOrganizationId()))
                .orElseThrow(UnauthorizedException::new);

        Page<User> userPage;
        if (StringUtils.isNotEmpty(searchKey)) {
            userPage = userRepository.searchUsersWithLocationRoleIdAndDeleteFlag("%" + searchKey + "%",
                    locationRoleId, false, getSortedByName);
        } else {
            userPage = userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRoleId, false, getSortedByName);
        }
        return CommonUtils.getPaginationResponse(userPage, UserMapper::entityToUserInfoForDropDown);

    }

    /**
     * @param userId id for which user info is required
     * @return User details for that user
     */
    public UserDetailsForAdminDto getMemberById(String userId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                        .flatMap(admin -> Optional.of(userOrganizationRepository.findOneByIdAndDeleteFlag(userId, false)
                                .map(user -> {
                                    if ((!user.getOrganizationId().equals(admin.getOrganizationId()) || (StringUtils.isNotEmpty(admin.getLocationId())
                                            && !admin.getLocationId().equals(user.getLocationId())))) {
                                        throw new UnauthorizedException();
                                    }
                                    return user;
                                })
                                .orElseThrow(() -> new EntityNotFoundException("user")))
                        )
                )
                .map(UserMapper::entityToUserDetailsForAdmin)
                .orElseThrow(AuthenticationException::new);
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
                                    if ((!user.getOrganizationId().equals(admin.getOrganizationId()) || (StringUtils.isNotEmpty(admin.getLocationId())
                                            && !admin.getLocationId().equals(user.getLocationId())))) {
                                        throw new UnauthorizedException();
                                    }
                                    return user.getLocationRole();
                                })
                                .map(locationRole -> {
                                    shiftSchedulingPreferences.set(ShiftSchedulingUtils.mappingFunction(locationRole));
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

    /**
     * @param userId         user for whom preferences are required
     * @param getFullHistory boolean to show if all past preferences should be returned too.
     * @return List of preferences or  single preference
     */
    public Object getEmployeePreferences(String userId, boolean getFullHistory) {
        if (SecurityUtils.isCurrentUserInRole(Authority.EMPLOYEE)) {
            userId = SecurityUtils.getCurrentUserLogin()
                    .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                            .map(User::getId)
                    )
                    .orElseThrow(AuthenticationException::new);
        }
        if (getFullHistory) {
            return employeePreferencesRepository.findAllByUserId(userId)
                    .stream().map(UserMapper::employeePreferenceToDto)
                    .collect(Collectors.toList());
        } else {
            return employeePreferencesRepository
                    .findOneByUserIdAndIsExpired(userId, false)
                    .map(UserMapper::employeePreferenceToDto)
                    .orElseThrow(() -> new EntityNotFoundException("preferences"));
        }
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
            throw new EntityAlreadyExistsException("email", emails.get(0));
        List<UserTokens> tokenList = new ArrayList<>();
        List<InviteUserMailDto> mailDtoList = inviteUserDtoList.stream()
                .filter(user -> emailAlreadyExist.contains(user.getEmail()))
                .map(UserMapper::inviteUserDtoToEntity)
                .map(user -> {
                    user.setCreatedBy(admin.getId());
                    user.setOrganizationId(admin.getOrganization().getId());
                    if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER)) {
                        user.setLocationId(admin.getLocationId());
                    }
                    UserTokens token = new UserTokens(UserToken.INVITE);
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

}