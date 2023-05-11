package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.locationandroles.LocationUsers;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.locationandroledtos.LocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.UpdateLocationRoleDto;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.LocationAndRoleMapper;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.locationandroles.LocationRoleRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.locationandroles.LocationUsersRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.*;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
@AllArgsConstructor
public class LocationAndRoleService {

    private final Logger log = LogManager.getLogger(LocationAndRoleService.class);

    private final LocationRepository locationRepository;

    private final LocationUsersRepository locationUsersRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final AppCustomIdConfig appCustomIdConfig;

    /**
     * Checks for logged-in employees location and return accordingly
     *
     * @return All locations under logged-in admin organization
     */
    public PaginationDto getLocation(int pageNo, int pageSize, String searchKey, Boolean active, String timezone) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("name").ascending());
        AtomicReference<String> userTimezone = new AtomicReference<>(null);
        Page<LocationUsers> locationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    userTimezone.set(user.getOrganization().getTimezone());
                    return locationUsersRepository.searchAndFilterLocation(searchKey, active, timezone, user.getOrganizationId(), getSortedByName);
                })
                .orElseThrow(AuthenticationException::new);
        return CommonUtils.getPaginationResponse(locationPage, LocationAndRoleMapper::entityToDto, userTimezone.get());
    }

    /**
     * Saves new Location into the database
     *
     * @param locationDto New location information
     */
    public void addNewLocation(LocationDto locationDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> {
                    locationRepository.getByNameAndOrganizationId(locationDto.getName(), admin.getOrganizationId())
                            .map(location -> {
                                throw new EntityAlreadyExistsException("location", "name", locationDto.getName());
                            });
                    return admin;
                })
                .map(admin -> LocationAndRoleMapper.dtoToEntity(locationDto, admin))
                .map(location -> {
                    String currentWaawId = locationRepository.getLastUsedWaawId()
                            .orElse(appCustomIdConfig.getLocationPrefix() + "0000000000");
                    location.setWaawId(CommonUtils.getNextCustomId(currentWaawId, appCustomIdConfig.getLength()));
                    return location;
                })
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
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        log.info("Deleting location with id: {}", id);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> locationRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(location -> {
                            if (location.getOrganizationId().equals(admin.getOrganizationId())) {
                                int roles = (int) locationRoleRepository.findAllByLocationIdAndDeleteFlag(location.getId(), false)
                                        .stream().filter(LocationRole::isActive)
                                        .count();
                                if (roles > 0) throw new EntityNotDeletableException("location", "roles and employees");
                                location.setDeleteFlag(true);
                                return location;
                            }
                            return null;
                        })
                        .map(locationRepository::save)
                        .orElseThrow(() -> new EntityNotFoundException("location"))
                );
        log.info("Successfully deleted the location and suspended all users for the location: {}", id);
    }

    /**
     * Toggle active/disabled location status
     *
     * @param id location id
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleActiveLocation(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> locationRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(location -> {
                            if (location.getOrganizationId().equals(admin.getOrganizationId())) {
                                int roles = (int) locationRoleRepository.findAllByLocationIdAndDeleteFlag(location.getId(), false)
                                        .stream().filter(LocationRole::isActive)
                                        .count();
                                if (roles > 0) throw new EntityNotDeletableException("location", "roles and employees");
                                log.info("Updating location({}) from {} to {}", location.getName(), location.isActive(), !location.isActive());
                                location.setActive(!location.isActive());
                                return location;
                            }
                            return null;
                        })
                        .map(locationRepository::save)
                        .map(location -> CommonUtils.logMessageAndReturnObject(location, "info", LocationAndRoleService.class,
                                "Successfully updated the location({}) from {} to {}", location.getName(), !location.isActive(), location.isActive()))
                        .orElseThrow(() -> new EntityNotFoundException("location"))
                );
    }

    /**
     * Checks for logged-in employee location role and return accordingly
     *
     * @return All location roles under logged-in admin organization
     */
    public PaginationDto getLocationRoles(int pageNo, int pageSize, String searchKey, Boolean active, String locationId,
                                          Boolean admin, String startDate, String endDate) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("name").ascending());
        AtomicReference<String> timezone = new AtomicReference<>(null);
        Page<LocationRole> locationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    Boolean isAdmin = admin;
                    String finalLocationId = locationId;
                    timezone.set(user.getAuthority().equals(Authority.ADMIN) ? user.getOrganization().getTimezone() :
                            user.getLocation().getTimezone());
                    Instant[] dateRange = StringUtils.isNotEmpty(startDate) ?
                            DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone.get()) :
                            new Instant[]{null, null};
                    if (user.getAuthority().equals(Authority.MANAGER)) {
                        finalLocationId = user.getLocationId();
                        isAdmin = false;
                    }
                    return locationRoleRepository.searchAndFilterRole(searchKey, user.getOrganizationId(), finalLocationId,
                            dateRange[0], dateRange[1], isAdmin, active, getSortedByName);
                })
                .orElseThrow(AuthenticationException::new);
        return CommonUtils.getPaginationResponse(locationPage, LocationAndRoleMapper::entityToDto, timezone.get());
    }

    /**
     * @param id role id for which info is required
     * @return Role Info
     */
    public LocationRoleDto getLocationRoleById(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(loggedUser -> locationRoleRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(role -> {
                            if (!role.getOrganizationId().equals(loggedUser.getOrganizationId()) ||
                                    (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                                            !role.getLocationId().equals(loggedUser.getLocationId()))) {
                                return null;
                            }
                            return LocationAndRoleMapper.entityToMainDto(role);
                        })
                )
                .orElseThrow(() -> new EntityNotFoundException("role"));
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
                .map(admin -> {
                    if (admin.getAuthority().equals(Authority.MANAGER))
                        locationRoleDto.setLocationId(admin.getLocationId());
                    locationRoleRepository.getByNameAndLocationId(locationRoleDto.getName(), locationRoleDto.getLocationId())
                            .map(location -> {
                                throw new EntityAlreadyExistsException("role", "name", locationRoleDto.getName());
                            });
                    return admin;
                })
                .map(admin -> LocationAndRoleMapper.dtoToEntity(locationRoleDto, admin))
                .map(locationRole -> {
                    String currentWaawId = locationRoleRepository.getLastUsedWaawId()
                            .orElse(appCustomIdConfig.getRolePrefix() + "0000000000");
                    System.out.println(currentWaawId);
                    locationRole.setWaawId(CommonUtils.getNextCustomId(currentWaawId, appCustomIdConfig.getLength()));
                    log.info("Adding new role: {}", locationRole);
                    return locationRole;
                })
                .map(locationRoleRepository::save)
                .map(locationRole -> CommonUtils.logMessageAndReturnObject(locationRole, "info", LocationAndRoleService.class,
                        "New Location role saved successfully: {}", locationRole))
                .orElseThrow(() -> new EntityNotFoundException("location"));
    }

    /**
     * Marks a location role as deleted in the database and suspend all associated users
     *
     * @param id id for the location to be deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLocationRole(String id) {
        log.info("Deleting location role with id: {}", id);
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> locationRoleRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(locationRole -> {
                            if (!locationRole.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    admin.getAuthority().equals(Authority.MANAGER) &&
                                            !locationRole.getLocationId().equals(admin.getLocationId()) &&
                                            locationRole.isAdminRights()) {
                                return null;
                            }
                            int employees = (int) userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRole.getId(), false)
                                    .stream().filter(emp -> emp.getAccountStatus().equals(AccountStatus.PAID_AND_ACTIVE) ||
                                            emp.getAccountStatus().equals(AccountStatus.INVITED))
                                    .count();
                            if (employees > 0) throw new EntityNotDeletableException("role", "employees");
                            locationRole.setDeleteFlag(true);
                            return locationRole;
                        })
                        .map(locationRoleRepository::save)
                );
        log.info("Successfully deleted the location role and suspended all users for the location: {}", id);
    }

    /**
     * Toggle active/disabled location role status
     *
     * @param id location role id
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleActiveLocationRole(String id) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> locationRoleRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(locationRole -> {
                            if (!locationRole.getOrganizationId().equals(admin.getOrganizationId()) ||
                                    admin.getAuthority().equals(Authority.MANAGER) &&
                                            !locationRole.getLocationId().equals(admin.getLocationId()) &&
                                            locationRole.isAdminRights()) {
                                return null;
                            }
                            if (!locationRole.isActive()) {
                                boolean activeParent = locationRepository.findOneByIdAndDeleteFlag(locationRole.getLocationId(), false)
                                        .filter(Location::isActive)
                                        .isPresent();
                                if (!activeParent) throw new EntityCantBeActivatedException("role", "location");
                            } else {
                                int employees = (int) userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRole.getId(), false)
                                        .stream().filter(emp -> emp.getAccountStatus().equals(AccountStatus.PAID_AND_ACTIVE) ||
                                                emp.getAccountStatus().equals(AccountStatus.INVITED))
                                        .count();
                                if (employees > 0) throw new EntityNotDeletableException("role", "employees");
                            }
                            log.info("Updating locationRole({}) from {} to {}", locationRole.getName(), locationRole.isActive(), !locationRole.isActive());
                            locationRole.setActive(!locationRole.isActive());
                            return locationRole;
                        })
                        .map(locationRoleRepository::save)
                        .map(locationRole -> CommonUtils.logMessageAndReturnObject(locationRole, "info", LocationAndRoleService.class,
                                "Successfully updated the locationRole({}) from {} to {}", locationRole.getName(), locationRole.isActive(), !locationRole.isActive()))
                        .orElseThrow(() -> new EntityNotFoundException("role"))
                );
    }

    /**
     * Update the location role preferences
     *
     * @param locationRoleDto details to update
     */
    public void updateLocationRolePreferences(UpdateLocationRoleDto locationRoleDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(admin -> locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleDto.getId(), false)
                        .map(locationRole -> {
                            boolean error = false;
                            if (admin.getAuthority().equals(Authority.ADMIN) && !locationRole.getOrganizationId().equals(admin.getOrganizationId())) {
                                error = true;
                            } else if (admin.getAuthority().equals(Authority.MANAGER) && !locationRole.getLocationId().equals(admin.getLocationId())) {
                                error = true;
                            }
                            if (error) throw new EntityNotFoundException("role");
                            return locationRole;
                        })
                )
                .map(locationRole -> {
                    LocationAndRoleMapper.updateDtoToEntity(locationRoleDto, locationRole);
                    return locationRole;
                })
                .map(locationRoleRepository::save)
                .map(locationRole -> CommonUtils.logMessageAndReturnObject(locationRole, "info", LocationAndRoleService.class,
                        "Location role updated successfully: {}", locationRole))
                .orElseThrow(() -> new EntityNotFoundException("role"));
    }

}