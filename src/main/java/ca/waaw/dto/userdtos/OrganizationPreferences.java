package ca.waaw.dto.userdtos;

import ca.waaw.enumration.report.PayrollGenerationType;
import ca.waaw.web.rest.utils.customannotations.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(allowableValues = {"WEEKLY", "MID_MONTH", "MONTHLY"})
    @ValueOfEnum(enumClass = PayrollGenerationType.class)
    private String payrollGenerationFrequency;

    private int clockInAllowedMinutesBeforeShift;

}