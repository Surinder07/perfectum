package ca.waaw.dto.shifts;

import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDetailsDto {

    private String id;

    private String batchName;

    @Schema(description = "Will be populated if batch is created for particular users")
    private List<UserInfoForDropDown> users;

    @Schema(description = "If batch is for a particular location, it will be populated or if batch is for a role, it will be populated")
    private LocationAndRoleDto locationAndRoleDetails;

    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String startDate;

    @Schema(description = "Date Format: <b>yyyy/MM/dd</b>")
    private String endDate;

    private boolean isReleased;

    private UserInfoForDropDown batchCreatedBy;

}
