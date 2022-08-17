package ca.waaw.dto.locationandroledtos;

import ca.waaw.web.rest.utils.customannotations.CapitalizeFirstLetter;
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

    private int totalHoursPerDayMin;

    private int totalHoursPerDayMax;

    private int minHoursBetweenShifts;

    private int maxConsecutiveWorkDays;

}