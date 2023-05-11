package ca.waaw.dto.userdtos;

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

    @Schema(hidden = true)
    private String id;

    @NotEmpty
    private String userId;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String mondayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String mondayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String tuesdayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String tuesdayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String wednesdayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String wednesdayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String thursdayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String thursdayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String fridayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String fridayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String saturdayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String saturdayEndTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String sundayStartTime;

    @ValidateRegex(type = RegexValidatorType.TIME, message = "Pass a valid time")
    @Schema(description = "Time Format: <b>24 hours (HH:MM)</b>")
    private String sundayEndTime;

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
