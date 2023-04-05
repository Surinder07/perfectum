package ca.waaw.domain;

import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.PayrollGenerationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "organization")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Organization extends AbstractEntity {

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_of_week")
    private DaysOfWeek firstDayOfWeek = DaysOfWeek.MONDAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payroll_generation_frequency")
    private PayrollGenerationType payrollGenerationFrequency = PayrollGenerationType.MONTHLY;

    @Column(name = "payment_pending")
    private boolean paymentPending = false;

    @Column(name = "trial_end_date")
    private Instant trialEndDate;

    @Column(name = "is_timeclock_enabled")
    private boolean isTimeclockEnabledDefault;

    @Column(name = "is_timeoff_enabled")
    private boolean isTimeoffEnabledDefault;

    @Column(name = "is_overtime_enabled")
    private boolean isOvertimeRequestEnabled;

    @Column(name = "days_before_shifts_assigned")
    private int daysBeforeShiftsAssigned = 4;

    @Column(name = "is_paid_until")
    private Instant isPaidUntil;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @Column
    private String timezone;

}