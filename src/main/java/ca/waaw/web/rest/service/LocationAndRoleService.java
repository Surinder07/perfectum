package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.joined.LocationUsers;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.locationandroledtos.LocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.UpdateLocationRoleDto;
import ca.waaw.enumration.Authority;
import ca.waaw.mapper.LocationAndRoleMapper;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.LocationUsersRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LocationAndRoleService {

    private final Logger log = LogManager.getLogger(LocationAndRoleService.class);

    private final LocationRepository locationRepository;

    private final LocationUsersRepository locationUsersRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final UserRepository userRepository;

    private final AppCustomIdConfig appCustomIdConfig;

    /**
     * Checks for logged-in employees location and return accordingly
     *
     * @return All locations under logged-in admin organization
     */
    public PaginationDto getLocation(int pageNo, int pageSize) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("name").ascending());
        Page<LocationUsers> locationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> locationUsersRepository.findAllByOrganizationIdAndDeleteFlag(user.getOrganizationId(), false, getSortedByName))
                .orElseThrow(AuthenticationException::new);
        return CommonUtils.getPaginationResponse(locationPage, LocationAndRoleMapper::entityToDto);
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
                                location.setDeleteFlag(true);
                                return location;
                            }
                            return null;
                        })
                        .map(locationRepository::save)
                        .map(Location::getId)
                        .map(locationId -> {
                            List<LocationRole> roles = locationRoleRepository.findAllByLocationIdAndDeleteFlag(locationId, true)
                                    .stream().peek(role -> role.setDeleteFlag(true)).collect(Collectors.toList());
                            locationRoleRepository.saveAll(roles);
                            return locationId;
                        })
                        .map(locationId -> userRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                                .stream().peek(user -> user.setDeleteFlag(true)).collect(Collectors.toList())
                        ).map(userRepository::saveAll)
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
    public PaginationDto getLocationRoles(int pageNo, int pageSize) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN, Authority.MANAGER);
        Pageable getSortedByName = PageRequest.of(pageNo, pageSize, Sort.by("name").ascending());
        Page<LocationRole> locationPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    if (user.getAuthority().equals(Authority.ADMIN))
                        return locationRoleRepository.findAllByOrganizationIdAndDeleteFlag(user.getOrganizationId(), false, getSortedByName);
                    else
                        return locationRoleRepository.findAllByLocationIdAndDeleteFlagAndAdminRights(user.getLocationId(), false, false, getSortedByName);
                })
                .orElseThrow(AuthenticationException::new);
        return CommonUtils.getPaginationResponse(locationPage, LocationAndRoleMapper::entityToDto);
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
                            if (locationRole.getOrganizationId().equals(admin.getOrganizationId())) {
                                locationRole.setDeleteFlag(true);
                                return locationRole;
                            }
                            return null;
                        })
                        .map(locationRoleRepository::save)
                        .map(LocationRole::getId)
                        .map(locationRoleId -> userRepository.findAllByLocationRoleIdAndDeleteFlag(locationRoleId, false)
                                .stream().peek(user -> user.setDeleteFlag(true)).collect(Collectors.toList())
                        ).map(userRepository::saveAll)
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
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(admin -> locationRoleRepository.findOneByIdAndDeleteFlag(id, false)
                        .map(locationRole -> {
                            if (locationRole.getOrganizationId().equals(admin.getOrganizationId())) {
                                log.info("Updating locationRole({}) from {} to {}", locationRole.getName(), locationRole.isActive(), !locationRole.isActive());
                                locationRole.setActive(!locationRole.isActive());
                                return locationRole;
                            }
                            return null;
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