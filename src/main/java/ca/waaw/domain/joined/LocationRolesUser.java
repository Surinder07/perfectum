package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "location_role")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationRolesUser extends AbstractEntity {

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
    private int totalHoursPerDayMin;

    @Column(name = "total_hours_per_day_max")
    private int totalHoursPerDayMax;

    @Column(name = "min_hours_between_shifts")
    private int minHoursBetweenShifts;

    @Column(name = "max_consecutive_work_days")
    private int maxConsecutiveWorkDays;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid")
    private List<User> users;

}
