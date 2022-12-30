package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSchedulingPreferences {

    private String userId;

    private String locationRoleId;

    private int totalHoursPerDayMin;

    private int totalHoursPerDayMax;

    private int minHoursBetweenShifts;

    private int maxConsecutiveWorkDays;

}
