package ca.waaw.mapper;

import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.LocationRolesUser;
import ca.waaw.dto.locationandroledtos.BaseLocationRole;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.LocationRoleWithUsersDto;
import ca.waaw.dto.userdtos.BaseUserDetails;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.EntityStatus;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocationRoleMapper {

    public static LocationRoleDto entityToDto(LocationRole source) {
        LocationRoleDto target = new LocationRoleDto();
        BeanUtils.copyProperties(source, target);
        target.setIsTimeclockEnabled(source.isTimeclockEnabled());
        target.setIsTimeoffEnabled(source.isTimeoffEnabled());
        return target;
    }

    public static LocationRole dtoToEntity(LocationRoleDto source1, User source2) {
        LocationRole target = new LocationRole();
        BeanUtils.copyProperties(source1, target);
        target.setId(UUID.randomUUID().toString());
        target.setLocationId(source2.getAuthority().equals(Authority.ADMIN)? source1.getLocationId() : null);
        target.setOrganizationId(source2.getOrganizationId());
        target.setCreatedBy(source2.getId());
        target.setStatus(EntityStatus.ACTIVE);
        target.setTimeclockEnabled(source1.getIsTimeclockEnabled());
        target.setTimeoffEnabled(source1.getIsTimeoffEnabled());
        return target;
    }

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

    public static BaseLocationRole entityToDtoSimple(LocationRole source) {
        BaseLocationRole target = new BaseLocationRole();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    public static void updateDtoToEntity(LocationRoleDto source, LocationRole target) {
        if (source.getIsTimeoffEnabled() != null) target.setTimeoffEnabled(source.getIsTimeoffEnabled());
        if (source.getIsTimeclockEnabled() != null) target.setTimeclockEnabled(source.getIsTimeclockEnabled());
        if (source.getTotalHoursPerDayMin() != 0) target.setTotalHoursPerDayMin(source.getTotalHoursPerDayMin());
        if (source.getTotalHoursPerDayMax() != 0) target.setTotalHoursPerDayMax(source.getTotalHoursPerDayMax());
        if (source.getMinHoursBetweenShifts() != 0) target.setMinHoursBetweenShifts(source.getMinHoursBetweenShifts());
        if (source.getMaxConsecutiveWorkDays() != 0) target.setMaxConsecutiveWorkDays(source.getMaxConsecutiveWorkDays());
    }

}
