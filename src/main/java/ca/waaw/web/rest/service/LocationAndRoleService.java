package ca.waaw.web.rest.service;

import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.dto.locationandroledtos.*;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.LocationMapper;
import ca.waaw.mapper.LocationRoleMapper;
import ca.waaw.repository.*;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LocationAndRoleService {

    private final Logger log = LogManager.getLogger(LocationAndRoleService.class);

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final LocationAndRolesRepository locationAndRolesRepository;

    private final LocationRolesUserRepository locationRolesUserRepository;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    /**
     * Checks if logged in employee is admin or employee and then returns accordingly
     *
     * @return For Global Admin: All locations and roles under them, for Location Managers: Their location and all
     * roles under them, for Employees or Contractors: Their location and role information
     */
    public Object getLocation() {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER, Authority.CONTRACTOR, Authority.EMPLOYEE);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN)) {
                        return getAllLocations(user.getOrganizationId());
                    } else if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER)) {
                        return getLocationForAdmin(user.getLocationId());
                    } else {
                        return getLocationForEmployee(user.getLocationId(), user.getLocationRoleId());
                    }
                })
                .orElseThrow(AuthenticationException::new);
    }

    /**
     * Saves new Location into the database
     *
     * @param newLocationDto New location information
     */
    public void addNewLocation(NewLocationDto newLocationDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    locationRepository.getByNameAndOrganizationId(newLocationDto.getName(), admin.getOrganizationId())
                            .map(location -> {
                                throw new EntityAlreadyExistsException("Location.name", newLocationDto.getName());
                            });
                    return admin;
                })
                .map(admin -> LocationMapper.dtoToEntity(newLocationDto, admin))
                .map(locationRepository::save)
                .map(location -> CommonUtils.logMessageAndReturnObject(location, "info", LocationAndRoleService.class,
                        "New Location saved successfully: {}", location))
                .orElseThrow(AuthenticationException::new);
    }

    /**
     * Marks a location as deleted in the database and suspend all associated users
     *
     * @param id id for the location to be deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLocation(String id) {
        log.info("Deleting location with id: {}", id);
        locationRepository.findOneByIdAndDeleteFlag(id, false)
                .map(location -> {
                    location.setDeleteFlag(true);
                    return location;
                })
                .map(locationRepository::save)
                .map(Location::getId)
                .map(locationId -> userRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                        .stream().peek(user -> user.setAccountStatus(AccountStatus.SUSPENDED)).collect(Collectors.toList())
                ).map(userRepository::saveAll)
                .orElseThrow(() -> new EntityNotFoundException("location"));
        log.info("Successfully deleted the location and suspended all users for the location: {}", id);
    }

    /**
     * Saves new Location role into the database
     *
     * @param locationRoleDto New location role information
     */
    public void addNewLocationRole(LocationRoleDto locationRoleDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> checkLocationIdForAdminRole(admin, locationRoleDto))
                .map(admin -> {
                    locationRoleRepository.getByNameAndLocationId(locationRoleDto.getName(), locationRoleDto.getLocationId())
                            .map(location -> {
                                throw new EntityAlreadyExistsException("LocationRole.name", locationRoleDto.getName());
                            });
                    return admin;
                })
                .map(admin -> checkOrganizationDefaultAndMapDtoToEntity(admin, locationRoleDto))
                .map(locationRoleRepository::save)
                .map(locationRole -> CommonUtils.logMessageAndReturnObject(locationRole, "info", LocationAndRoleService.class,
                        "New Location role saved successfully: {}", locationRole))
                .orElseThrow(() -> new BadRequestException("LocationId is required for global admin"));
    }

    /**
     * Marks a location role as deleted in the database and suspend all associated users
     *
     * @param id id for the location to be deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLocationRole(String id) {
        log.info("Deleting location role with id: {}", id);
        locationRoleRepository.findOneByIdAndDeleteFlag(id, false)
                .map(locationRole -> {
                    locationRole.setDeleteFlag(true);
                    return locationRole;
                })
                .map(locationRoleRepository::save)
                .map(LocationRole::getId)
                .map(locationRoleId -> userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRoleId, false)
                        .stream().peek(user -> user.setAccountStatus(AccountStatus.SUSPENDED)).collect(Collectors.toList())
                ).map(userRepository::saveAll);
        log.info("Successfully deleted the location role and suspended all users for the location: {}", id);
    }

    /**
     * @param locationRoleId id to search location role in database
     * @return Location Role Info based on authority
     */
    public Object getLocationRoleInfo(String locationRoleId) {
        if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN, Authority.MANAGER) && StringUtils.isEmpty(locationRoleId)) {
            throw new BadRequestException("locationRoleId is required", "locationRoleId");
        } else if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN, Authority.MANAGER)) {
            return getAllLocationRoleInfo(locationRoleId);
        } else if (SecurityUtils.isCurrentUserInRole(Authority.EMPLOYEE, Authority.CONTRACTOR)) {
            return getAllLocationRoleInfo();
        }
        throw new UnauthorizedException();
    }

    /**
     * Update the location role preferences
     *
     * @param locationRoleDto details to update
     */
    public void updateLocationRolePreferences(LocationRoleDto locationRoleDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleDto.getId(), false)
                        .map(locationRole -> locationRole.getOrganizationId().equals(user.getOrganizationId()) ? locationRole : null)
                )
                .map(locationRole -> {
                    LocationRoleMapper.updateDtoToEntity(locationRoleDto, locationRole);
                    return locationRole;
                })
                .map(locationRoleRepository::save)
                .map(locationRole -> CommonUtils.logMessageAndReturnObject(locationRole, "info", LocationAndRoleService.class,
                        "Location role updated successfully: {}", locationRole))
                .orElseThrow(() -> new EntityNotFoundException("location role"));
    }

    /**
     * For employees and contractors
     *
     * @return Simple info about location role
     */
    private BaseLocationRole getAllLocationRoleInfo() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> locationRoleRepository.findOneByIdAndDeleteFlag(user.getLocationRoleId(), false))
                .map(LocationRoleMapper::entityToDtoSimple)
                .orElseThrow(AuthenticationException::new);
    }

    /**
     * For admins
     *
     * @param locationRoleId id for the locationRole to look in database
     * @return Location Role info with all users under it
     */
    private LocationRoleWithUsersDto getAllLocationRoleInfo(String locationRoleId) {
        return locationRolesUserRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                .flatMap(locationRolesUser -> SecurityUtils.getCurrentUserLogin()
                        .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false)
                                .map(user -> {
                                    if (!user.getOrganizationId().equalsIgnoreCase(locationRolesUser.getOrganizationId())) {
                                        return null;
                                    }
                                    return locationRolesUser;
                                })
                        )
                )
                .map(LocationRoleMapper::entityToDto)
                .orElseThrow(() -> new EntityNotFoundException("location role"));
    }

    /**
     * @return All locations and their roles for an organization
     */
    private List<AdminLocationDto> getAllLocations(String organizationId) {
        return locationAndRolesRepository.findAllByOrganizationIdAndDeleteFlag(organizationId, false)
                .stream().map(LocationMapper::entityToDtoForAdmin).collect(Collectors.toList());
    }

    /**
     * @return Location info with all location roles under that location
     */
    private AdminLocationDto getLocationForAdmin(String locationId) {
        return locationAndRolesRepository.findOneByIdAndDeleteFlag(locationId, false)
                .map(LocationMapper::entityToDtoForAdmin)
                .orElseThrow(() -> new EntityNotFoundException("location"));
    }

    /**
     * @return Location info and role for the single role assigned to the employee
     */
    private EmployeeLocationDto getLocationForEmployee(String locationId, String locationRoleId) {
        return locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                .flatMap(location -> locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                        .map(locationRole -> LocationMapper.entitiesToDtoForEmployee(location, locationRole))
                ).orElseThrow(() -> new EntityNotFoundException("location"));
    }

    /**
     * If admin is a global admin and location id is not provided in the request, it will return null,
     * so {@link Optional#orElseThrow()} will throw a bad request exception
     *
     * @param admin           admin adding the new location role
     * @param locationRoleDto details for location role
     * @return admin object received by method or null
     */
    private User checkLocationIdForAdminRole(User admin, LocationRoleDto locationRoleDto) {
        if (admin.getAuthority().equals(Authority.ADMIN) && StringUtils.isEmpty(locationRoleDto.getLocationId())) {
            return null;
        }
        return admin;
    }

    /**
     * If admin is location admin, location id will be updated same as admin, if timeclock and timeoff preference
     * are not given, they will be set according to organization default
     *
     * @param admin           admin adding the new location role
     * @param locationRoleDto details for location role
     * @return locationRole object received by method or null
     */
    private LocationRole checkOrganizationDefaultAndMapDtoToEntity(User admin, LocationRoleDto locationRoleDto) {
        if (admin.getAuthority().equals(Authority.MANAGER)) {
            locationRoleDto.setLocationId(admin.getLocationId());
        }
        LocationRole locationRole = LocationRoleMapper.dtoToEntity(locationRoleDto, admin);
        if (locationRoleDto.getIsTimeclockEnabled() == null || locationRoleDto.getIsTimeoffEnabled() == null) {
            organizationRepository.findOneByIdAndDeleteFlag(admin.getOrganizationId(), false)
                    .ifPresent(organization -> {
                        locationRole.setTimeclockEnabled(locationRoleDto.getIsTimeclockEnabled() == null ?
                                organization.isTimeclockEnabledDefault() : locationRoleDto.getIsTimeclockEnabled());
                        locationRole.setTimeoffEnabled(locationRoleDto.getIsTimeoffEnabled() == null ?
                                organization.isTimeoffEnabledDefault() : locationRoleDto.getIsTimeoffEnabled());
                    });
        }
        return locationRole;
    }

}