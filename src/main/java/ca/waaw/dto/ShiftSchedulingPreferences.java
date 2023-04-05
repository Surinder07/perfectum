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

    private int totalMinutesPerDayMin;

    private int totalMinutesPerDayMax;

    private int minMinutesBetweenShifts;

    private int maxConsecutiveWorkDays;

}
