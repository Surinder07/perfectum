package ca.waaw.dto.locationandroledtos;

import ca.waaw.dto.TimeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRoleDto {

    @NotEmpty
    private String id;

    @Schema(description = "minimum total hours an employee can work per day")
    private TimeDto totalHoursPerDayMin;

    @Schema(description = "maximum total hours an employee can work per day")
    private TimeDto totalHoursPerDayMax;

    @Schema(description = "minimum total hours an employee has to take between two shifts")
    private TimeDto minHoursBetweenShifts;

    @Schema(description = "maximum total consecutive days an employee can work")
    private int maxConsecutiveWorkDays;

}