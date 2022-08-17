package ca.waaw.web.rest.service;

import ca.waaw.domain.Location;
import ca.waaw.domain.User;
import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.locationandroledtos.EmployeeLocationDto;
import ca.waaw.dto.locationandroledtos.NewLocationDto;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.mapper.LocationMapper;
import ca.waaw.repository.LocationAndRolesRepository;
import ca.waaw.repository.LocationRepository;
import ca.waaw.repository.LocationRoleRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LocationService {

    private final Logger log = LogManager.getLogger(LocationService.class);

    private final LocationRepository locationRepository;

    private final LocationRoleRepository locationRoleRepository;

    private final LocationAndRolesRepository locationAndRolesRepository;

    private final UserRepository userRepository;

    /**
     * Checks if logged in employee is admin or employee and then returns accordingly
     *
     * @return For Global Admin: All locations and roles under them, for Location Managers: Their location and all
     * roles under them, for Employees or Contractors: Their location and role information
     */
    public Object getLocation() {
        User user = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        if (SecurityUtils.isCurrentUserInRole(Authority.ADMIN)) {
            return getAllLocations(user.getOrganizationId());
        } else if (SecurityUtils.isCurrentUserInRole(Authority.MANAGER)) {
            return getLocationAdmin(user.getLocationId());
        } else if (SecurityUtils.isCurrentUserInRole(Authority.EMPLOYEE) || SecurityUtils.isCurrentUserInRole(Authority.CONTRACTOR)) {
            return getLocationEmployee(user.getLocationId(), user.getLocationRoleId());
        }
        throw new UnauthorizedException();
    }

    /**
     * Saves new Location into the database
     *
     * @param newLocationDto New location information
     */
    public void addNewLocation(NewLocationDto newLocationDto) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        User admin = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        Location location = LocationMapper.dtoToEntity(newLocationDto, admin);
        locationRepository.save(location);
        log.info("New Location saved successfully: {}", location);
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
                }).map(locationRepository::save)
                .map(Location::getId)
                .map(locationId -> userRepository.findAllByLocationIdAndDeleteFlag(locationId, false)
                        .stream().peek(user -> user.setStatus(EntityStatus.SUSPENDED)).collect(Collectors.toList())
                ).map(userRepository::saveAll);
        log.info("Successfully deleted the location and suspended all users for the location: {}", id);
    }

    /**
     * @return All locations and their roles
     */
    private List<AdminLocationDto> getAllLocations(String organizationId) {
        return locationAndRolesRepository.findAllByOrganizationIdAndDeleteFlag(organizationId, false)
                .stream().map(LocationMapper::entityToDtoForAdmin).collect(Collectors.toList());
    }

    /**
     * @return Location info with all location roles under that location
     */
    private AdminLocationDto getLocationAdmin(String locationId) {
        return locationAndRolesRepository.findOneByIdAndDeleteFlag(locationId, false)
                .map(LocationMapper::entityToDtoForAdmin)
                .orElseThrow(() -> new EntityNotFoundException("location"));
    }

    /**
     * @return Location info and role for the single role assigned to the employee
     */
    private EmployeeLocationDto getLocationEmployee(String locationId, String locationRoleId) {
        return locationRepository.findOneByIdAndDeleteFlag(locationId, false)
                .flatMap(location -> locationRoleRepository.findOneByIdAndDeleteFlag(locationRoleId, false)
                        .map(locationRole -> LocationMapper.entitiesToDtoForEmployee(location, locationRole))
                ).orElseThrow(() -> new EntityNotFoundException("location"));
    }

}