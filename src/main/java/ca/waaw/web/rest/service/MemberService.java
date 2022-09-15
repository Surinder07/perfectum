package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppUrlConfig;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.EmployeePreferencesDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.userdtos.InviteUserDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.UserMapper;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.UserOrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.UserMailService;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MemberService {

    private final Logger log = LogManager.getLogger(MemberService.class);

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final AppUrlConfig appUrlConfig;

    private final UserMailService userMailService;

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

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
        return CommonUtils.getPaginationResponse(userPage, UserMapper::entityToUserInfoForDropDown);

    }

    /**
     *
     * @param employeePreferencesDto all employee preferences to be updated
     */
    public void addEmployeePreferences(EmployeePreferencesDto employeePreferencesDto) {
        
    }

    /**
     *
     * @param userId
     * @param getFullHistory
     * @return
     */
    public List<EmployeePreferencesDto> getEmployeePreferences(String userId, boolean getFullHistory) {
        return null;
    }

}
