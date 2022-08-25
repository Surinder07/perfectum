package ca.waaw.dto.userdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPreferences {

    private Boolean isTimeclockEnabledDefault;

    private Boolean isTimeoffEnabledDefault;

    private Boolean isOvertimeRequestEnabled;

    private Integer daysBeforeShiftsAssigned;

}