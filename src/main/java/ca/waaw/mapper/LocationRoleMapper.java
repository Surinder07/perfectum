package ca.waaw.mapper;

import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.LocationRolesUser;
import ca.waaw.dto.locationandroledtos.BaseLocationRole;
import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.LocationRoleWithUsersDto;
import ca.waaw.dto.userdtos.BaseUserDetails;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationRoleMapper {

    /**
     * @param source location role entity
     * @return location role dto with all info
     */
    public static LocationRoleDto entityToDto(LocationRole source) {
        LocationRoleDto target = new LocationRoleDto();
        BeanUtils.copyProperties(source, target);
        target.setIsTimeclockEnabled(source.isTimeclockEnabled());
        target.setIsTimeoffEnabled(source.isTimeoffEnabled());
        return target;
    }

    /**
     * @param source1 locationRole info
     * @param source2 loggedIn user info
     * @return Location role entity to be saved in database
     */
    public static LocationRole dtoToEntity(LocationRoleDto source1, User source2) {
        LocationRole target = new LocationRole();
        BeanUtils.copyProperties(source1, target);
        target.setLocationId(source2.getAuthority().equals(Authority.ADMIN) ? source1.getLocationId() : null);
        target.setOrganizationId(source2.getOrganizationId());
        target.setCreatedBy(source2.getId());
        target.setStatus(EntityStatus.ACTIVE);
        target.setTimeclockEnabled(source1.getIsTimeclockEnabled());
        target.setTimeoffEnabled(source1.getIsTimeoffEnabled());
        return target;
    }

    /**
     * @param source location role with user entity
     * @return detailed location role with user dto
     */
    public static LocationRoleWithUsersDto entityToDto(LocationRolesUser source) {
        LocationRoleWithUsersDto target = new LocationRoleWithUsersDto();
        BeanUtils.copyProperties(source, target);
        List<BaseUserDetails> userDetailList = new ArrayList<>();
        source.getUsers().forEach(user -> {
            BaseUserDetails userDetails = new BaseUserDetails();
            BeanUtils.copyProperties(user, userDetails);
            userDetailList.add(userDetails);
        });
        target.setUsers(userDetailList);
        return target;
    }

    /**
     * @param source location role entity
     * @return location role dto
     */
    public static BaseLocationRole entityToDtoSimple(LocationRole source) {
        BaseLocationRole target = new BaseLocationRole();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * @param source dto with location role info
     * @param target entity in which info will be updated
     */
    public static void updateDtoToEntity(LocationRoleDto source, LocationRole target) {
        if (source.getIsTimeoffEnabled() != null) target.setTimeoffEnabled(source.getIsTimeoffEnabled());
        if (source.getIsTimeclockEnabled() != null) target.setTimeclockEnabled(source.getIsTimeclockEnabled());
        if (source.getTotalHoursPerDayMin() != 0) target.setTotalHoursPerDayMin(source.getTotalHoursPerDayMin());
        if (source.getTotalHoursPerDayMax() != 0) target.setTotalHoursPerDayMax(source.getTotalHoursPerDayMax());
        if (source.getMinHoursBetweenShifts() != 0) target.setMinHoursBetweenShifts(source.getMinHoursBetweenShifts());
        if (source.getMaxConsecutiveWorkDays() != 0)
            target.setMaxConsecutiveWorkDays(source.getMaxConsecutiveWorkDays());
    }

    /**
     * @param locationSource     location entity
     * @param locationRoleSource location role entity
     * @return location and location role dto info with minimal details
     */
    public static LocationAndRoleDto locationEntityToDetailDto(Location locationSource, LocationRole locationRoleSource) {
        LocationAndRoleDto locationAndRoleInfo = new LocationAndRoleDto();
        locationAndRoleInfo.setLocationId(locationSource.getId());
        locationAndRoleInfo.setLocationName(locationSource.getName());
        locationAndRoleInfo.setLocationTimezone(locationSource.getTimezone());
        locationAndRoleInfo.setLocationRoleId(locationRoleSource.getId());
        locationAndRoleInfo.setLocationRoleName(locationRoleSource.getName());
        return locationAndRoleInfo;
    }

}
