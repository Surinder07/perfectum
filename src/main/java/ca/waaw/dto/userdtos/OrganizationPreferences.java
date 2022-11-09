package ca.waaw.dto.userdtos;

import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.ORGANIZATION_PREFERENCES_PAYROLL, message =
        "Please pass a valid day if frequency is weekly or send a value between 1 and 31 for monthly or mid monthly")
public class OrganizationPreferences {

    private Boolean isTimeclockEnabledDefault;

    private Boolean isTimeoffEnabledDefault;

    private Boolean isOvertimeRequestEnabled;

    private Integer daysBeforeShiftsAssigned;

    @Schema(allowableValues = {"WEEKLY", "MID_MONTH", "MONTHLY"})
    private String payrollGenerationFrequency;

    private String dayDateForPayroll;

}