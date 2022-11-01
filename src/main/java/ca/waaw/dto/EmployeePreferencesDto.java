package ca.waaw.dto;

import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.ValidateRegex;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.RegexValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.EMPLOYEE_PREFERENCES_WAGES, message =
        "Pass both Wages per hour with a valid currency or neither")
public class EmployeePreferencesDto {

    private String id;

    @NotEmpty
    private String userId;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String mondayStartTime;

    private float mondayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String tuesdayStartTime;

    private float tuesdayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String wednesdayStartTime;

    private float wednesdayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String thursdayStartTime;

    private float thursdayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String fridayStartTime;

    private float fridayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String saturdayStartTime;

    private float saturdayWorkingHours;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String sundayStartTime;

    private float sundayWorkingHours;

    @Schema(description = "wages per hour for payroll purposes")
    private float wagesPerHour;

    private String wagesCurrency;

    @Schema(hidden = true)
    private String createdBy;

    @Schema(hidden = true)
    private Instant createdTime;

    @Schema(hidden = true)
    private boolean isActive;

}
