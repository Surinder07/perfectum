package ca.waaw.mapper;

import ca.waaw.domain.LocationRole;
import ca.waaw.dto.locationandroledtos.LocationRoleDto;
import org.springframework.beans.BeanUtils;

public class LocationRoleMapper {

    public static LocationRoleDto entityToDto(LocationRole source) {
        LocationRoleDto target = new LocationRoleDto();
        BeanUtils.copyProperties(source, target);
        return target;
    }

}
