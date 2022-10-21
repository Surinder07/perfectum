package ca.waaw.dto.timeoff;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeOffInfoDto {

    private String id;

    private DateTimeDto startDate;

    private DateTimeDto endDate;

    private String note;

    private String status;

    private UserInfoForDropDown user;

    private LocationAndRoleDto locationAndRole;
}
