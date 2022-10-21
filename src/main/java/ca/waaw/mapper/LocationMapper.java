package ca.waaw.mapper;

import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.LocationAndRoles;
import ca.waaw.dto.locationandroledtos.AdminLocationDto;
import ca.waaw.dto.locationandroledtos.EmployeeLocationDto;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import ca.waaw.dto.locationandroledtos.NewLocationDto;
import ca.waaw.enumration.EntityStatus;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationMapper {

    /**
     * @param source     New location information we get from UI
     * @param userSource Logged-in user, creating the location
     * @return Location entity to be saved in the database
     */
    public static Location dtoToEntity(NewLocationDto source, User userSource) {
        Location target = new Location();
        target.setName(source.getName());
        target.setTimezone(source.getTimezone());
        target.setOrganizationId(userSource.getOrganizationId());
        target.setCreatedBy(userSource.getId());
        target.setStatus(EntityStatus.ACTIVE);
        return target;
    }

    /**
     * @param source {@link LocationAndRoles} entity containing all location and role info
     * @return Location info with a list of roles present under that location
     */
    public static AdminLocationDto entityToDtoForAdmin(LocationAndRoles source) {
        AdminLocationDto target = new AdminLocationDto();
        BeanUtils.copyProperties(source, target);
        List<LocationRoleDto> rolesInfo = new ArrayList<>();
        source.getLocationRoles().forEach(roleInfo -> rolesInfo.add(LocationRoleMapper.entityToDto(roleInfo)));
        target.setLocationRoles(rolesInfo);
        return target;
    }

    /**
     * @param source1 Location info
     * @param source2 LocationRole info
     * @return Location and role info for the single employee
     */
    public static EmployeeLocationDto entitiesToDtoForEmployee(Location source1, LocationRole source2) {
        EmployeeLocationDto target = new EmployeeLocationDto();
        BeanUtils.copyProperties(source1, target);
        target.setLocationRoles(LocationRoleMapper.entityToDto(source2));
        return target;
    }

}
