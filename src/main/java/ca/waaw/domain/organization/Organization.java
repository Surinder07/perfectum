package ca.waaw.domain.organization;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.DaysOfWeek;
import ca.waaw.enumration.report.PayrollGenerationType;
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

    @Column(name = "image_file")
    private String imageFile;

    @Column(name = "is_timeclock_enabled")
    private boolean isTimeclockEnabledDefault = true;

    @Column(name = "is_timeoff_enabled")
    private boolean isTimeoffEnabledDefault = true;

    @Column(name = "is_overtime_enabled")
    private boolean isOvertimeRequestEnabled = true;

    @Column(name = "days_before_shifts_assigned")
    private int daysBeforeShiftsAssigned = 4;

    @Column(name="clock_in_allowed_minutes_before_shift")
    private int clockInAllowedMinutesBeforeShift;

    @Column(name = "next_payment_on")
    private Instant nextPaymentOn;

    @Column(name = "platform_fee_paid")
    private boolean platformFeePaid;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @Column
    private String timezone;

}