package ca.waaw.dto;

import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDetailDto {

    private String id;

    private DateTimeDto start;

    private DateTimeDto end;

    private UserInfoForDropDown user;

    private LocationAndRoleDto locationAndRole;

}
