package ca.waaw.mapper;

import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.LocationUsers;
import ca.waaw.dto.locationandroledtos.*;
import ca.waaw.web.rest.utils.CommonUtils;
import org.springframework.beans.BeanUtils;

public class LocationAndRoleMapper {

    /**
     * @param sourceLocation new location info
     * @param sourceAdmin    logged-in user info
     * @return Location entity to be saved in the database
     */
    public static Location dtoToEntity(LocationDto sourceLocation, User sourceAdmin) {
        Location target = new Location();
        target.setTimezone(sourceLocation.getTimezone());
        target.setName(sourceLocation.getName());
        target.setOrganizationId(sourceAdmin.getOrganizationId());
        target.setCreatedBy(sourceAdmin.getId());
        return target;
    }

    /**
     * @param source Location detail with all users
     * @return Dto to be sent for API
     */
    public static LocationDetailedDto entityToDto(LocationUsers source) {
        LocationDetailedDto target = new LocationDetailedDto();
        target.setId(source.getId());
        target.setWaawId(source.getWaawId());
        target.setTimezone(source.getTimezone());
        target.setName(source.getName());
        target.setActive(source.isActive());
        target.setCreationDate(source.getCreatedDate().toString().split("T")[0]);
        int activeEmployees = CommonUtils.getActiveEmployeesFromList(source.getUsers());
        target.setActiveEmployees(activeEmployees);
        target.setInactiveEmployees(source.getUsers().size() - activeEmployees);
        return target;
    }

    /**
     * @param sourceRole  locationRole info
     * @param sourceAdmin loggedIn user info
     * @return Location role entity to be saved in database
     */
    public static LocationRole dtoToEntity(LocationRoleDto sourceRole, User sourceAdmin) {
        LocationRole target = new LocationRole();
        target.setName(sourceRole.getName());
        target.setLocationId(sourceRole.getLocationId());
        target.setOrganizationId(sourceAdmin.getOrganizationId());
        target.setCreatedBy(sourceAdmin.getId());
        target.setAdminRights(sourceRole.isAdmin());
        if (sourceRole.getMaxConsecutiveWorkDays() != 0)
            target.setMaxConsecutiveWorkDays(sourceRole.getMaxConsecutiveWorkDays());
        if (sourceRole.getMinHoursBetweenShifts() != 0)
            target.setMinHoursBetweenShifts(sourceRole.getMinHoursBetweenShifts());
        if (sourceRole.getTotalHoursPerDayMin() != 0)
            target.setTotalHoursPerDayMin(sourceRole.getTotalHoursPerDayMin());
        if (sourceRole.getTotalHoursPerDayMax() != 0)
            target.setTotalHoursPerDayMax(sourceRole.getTotalHoursPerDayMax());
        return target;
    }

    /**
     * @param source location role info to be updated
     * @return Dto to be returned as API response
     */
    public static LocationRoleDetailedDto entityToDto(LocationRole source) {
        LocationRoleDetailedDto target = new LocationRoleDetailedDto();
        target.setId(source.getId());
        target.setWaawId(source.getWaawId());
        target.setName(source.getName());
        target.setCreationDate(source.getCreatedDate().toString().split("T")[0]);
        target.setActive(source.isActive());
        target.setAdmin(source.isAdminRights());
        target.setLocation(source.getLocation().getName());
        target.setCreatedBy(source.getCreatedByUser().getFullName());
        return target;
    }

    /**
     *
     * @param source location role info to be updated
     * @return Dto with preferences to be returned as API response
     */
    public static LocationRoleDto entityToMainDto(LocationRole source) {
        LocationRoleDto target = new LocationRoleDto();
        BeanUtils.copyProperties(source, target);
        target.setAdmin(source.isAdminRights());
        return target;
    }

    /**
     * @param source dto with location role info
     * @param target entity in which info will be updated
     */
    public static void updateDtoToEntity(UpdateLocationRoleDto source, LocationRole target) {
        if (source.getTotalHoursPerDayMin() != 0) target.setTotalHoursPerDayMin(source.getTotalHoursPerDayMin());
        if (source.getTotalHoursPerDayMax() != 0) target.setTotalHoursPerDayMax(source.getTotalHoursPerDayMax());
        if (source.getMinHoursBetweenShifts() != 0) target.setMinHoursBetweenShifts(source.getMinHoursBetweenShifts());
        if (source.getMaxConsecutiveWorkDays() != 0)
            target.setMaxConsecutiveWorkDays(source.getMaxConsecutiveWorkDays());
    }

}
