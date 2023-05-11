package ca.waaw.mapper;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.user.User;
import ca.waaw.domain.locationandroles.LocationUsers;
import ca.waaw.dto.locationandroledtos.*;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
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
    public static LocationDetailedDto entityToDto(LocationUsers source, String timezone) {
        LocationDetailedDto target = new LocationDetailedDto();
        target.setId(source.getId());
        target.setWaawId(source.getWaawId());
        target.setTimezone(source.getTimezone());
        target.setName(source.getName());
        target.setActive(source.isActive());
        target.setCreationDate(DateAndTimeUtils.getFullMonthDate(source.getCreatedDate(), timezone));
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
        target.setTotalMinutesPerDayMin(DateAndTimeUtils.getTotalMinutesForTime(sourceRole.getTotalHoursPerDayMin()));
        target.setTotalMinutesPerDayMax(DateAndTimeUtils.getTotalMinutesForTime(sourceRole.getTotalHoursPerDayMax()));
        target.setMinMinutesBetweenShifts(DateAndTimeUtils.getTotalMinutesForTime(sourceRole.getMinHoursBetweenShifts()));
        return target;
    }

    /**
     * @param source location role info to be updated
     * @return Dto to be returned as API response
     */
    public static LocationRoleDetailedDto entityToDto(LocationRole source, String timezone) {
        LocationRoleDetailedDto target = new LocationRoleDetailedDto();
        target.setId(source.getId());
        target.setWaawId(source.getWaawId());
        target.setName(source.getName());
        target.setCreationDate(DateAndTimeUtils.getFullMonthDate(source.getCreatedDate(), timezone));
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
        target.setMinHoursBetweenShifts(DateAndTimeUtils.getHourMinuteTimeFromMinutes(source.getMinMinutesBetweenShifts()));
        target.setTotalHoursPerDayMin(DateAndTimeUtils.getHourMinuteTimeFromMinutes(source.getTotalMinutesPerDayMin()));
        target.setTotalHoursPerDayMax(DateAndTimeUtils.getHourMinuteTimeFromMinutes(source.getTotalMinutesPerDayMax()));
        return target;
    }

    /**
     * @param source dto with location role info
     * @param target entity in which info will be updated
     */
    public static void updateDtoToEntity(UpdateLocationRoleDto source, LocationRole target) {
        target.setTotalMinutesPerDayMin(DateAndTimeUtils.getTotalMinutesForTime(source.getTotalHoursPerDayMin()));
        target.setTotalMinutesPerDayMax(DateAndTimeUtils.getTotalMinutesForTime(source.getTotalHoursPerDayMax()));
        target.setMinMinutesBetweenShifts(DateAndTimeUtils.getTotalMinutesForTime(source.getMinHoursBetweenShifts()));
        target.setMaxConsecutiveWorkDays(source.getMaxConsecutiveWorkDays());
    }

}
