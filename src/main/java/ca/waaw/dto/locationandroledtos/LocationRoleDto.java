package ca.waaw.dto.locationandroledtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import ca.waaw.web.rest.utils.customannotations.ValidateDependentDtoField;
import ca.waaw.web.rest.utils.customannotations.helperclass.enumuration.DependentDtoFieldsValidatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidateDependentDtoField(type = DependentDtoFieldsValidatorType.LOCATION_ROLE_TO_USER_ROLE,
        message = "Location is required")
public class LocationRoleDto {

    @Schema(description = "Needed in case of global admin, Not needed while updating location role")
    private String locationId;

    @Schema(hidden = true)
    private String waawId;

    @NotEmpty
    @CapitalizeFirstLetter
    @Schema(description = "non updatable")
    private String name;

    @Schema(description = "minimum total hours an employee can work per day")
    private int totalHoursPerDayMin;

    @Schema(description = "maximum total hours an employee can work per day")
    private int totalHoursPerDayMax;

    @Schema(description = "minimum total hours an employee has to take between two shifts")
    private int minHoursBetweenShifts;

    @Schema(description = "maximum total consecutive days an employee can work")
    private int maxConsecutiveWorkDays;

    private boolean isAdmin;

}