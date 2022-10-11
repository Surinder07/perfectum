package ca.waaw.dto.shifts;

import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDetailsDto {

    private String id;

    @Schema(description = "Will be null if shift is not assigned")
    private UserInfoForDropDown user;

    private LocationAndRoleDto locationAndRoleDetails;

    private DateTimeDto start;

    private DateTimeDto end;

    private String notes;

    private ShiftStatus shiftStatus;

    private ShiftType shiftType;

    @Schema(description = "Will be null if there are no conflicts")
    private List<String> conflicts;

}