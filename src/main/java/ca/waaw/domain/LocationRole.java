package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@Table(name = "location_role")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationRole extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "is_timeclock_enabled")
    private boolean isTimeclockEnabled;

    @Column(name = "is_timeoff_enabled")
    private boolean isTimeoffEnabled;

    @Column(name = "total_hours_per_day_min")
    private int totalHoursPerDayMin = 4;

    @Column(name = "total_hours_per_day_max")
    private int totalHoursPerDayMax = 8;

    @Column(name = "min_hours_between_shifts")
    private int minHoursBetweenShifts = 12;

    @Column(name = "max_consecutive_work_days")
    private int maxConsecutiveWorkDays = 6;

}