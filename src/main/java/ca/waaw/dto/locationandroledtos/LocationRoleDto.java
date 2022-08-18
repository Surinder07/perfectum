package ca.waaw.dto.locationandroledtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRoleDto {

    private String id;

    @NotEmpty
    @CapitalizeFirstLetter
    private String name;

    private boolean isTimeclockEnabled;

    private boolean isTimeoffEnabled;

    @Schema(description = "minimum total hours an employee can work per day")
    private int totalHoursPerDayMin;

    @Schema(description = "maximum total hours an employee can work per day")
    private int totalHoursPerDayMax;

    @Schema(description = "minimum total hours an employee has to take between two shifts")
    private int minHoursBetweenShifts;

    @Schema(description = "maximum total consecutive days an employee can work")
    private int maxConsecutiveWorkDays;

}