package ca.waaw.domain;

import ca.waaw.enumration.DaysOfWeek;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@Table(name = "recurring_shifts")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RecurringShifts extends AbstractEntity {

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DaysOfWeek dayOfWeek;

    @Column(name = "start_hour")
    private int startHour;

    @Column(name = "start_minute")
    private int startMinute;

    @Column(name = "end_hour")
    private int endHour;

    @Column(name = "end_minute")
    private int endMinute;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column(name = "available_quantity")
    private int availableQuantity;

}