package ca.waaw.domain.user;

import ca.waaw.enumration.Currency;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "employee_preferences")
public class EmployeePreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "monday_start_time")
    private String mondayStartTime;

    @Column(name = "monday_end_time")
    private String mondayEndTime;

    @Column(name = "tuesday_start_time")
    private String tuesdayStartTime;

    @Column(name = "tuesday_end_time")
    private String tuesdayEndTime;

    @Column(name = "wednesday_start_time")
    private String wednesdayStartTime;

    @Column(name = "wednesday_end_time")
    private String wednesdayEndTime;

    @Column(name = "thursday_start_time")
    private String thursdayStartTime;

    @Column(name = "thursday_end_time")
    private String thursdayEndTime;

    @Column(name = "friday_start_time")
    private String fridayStartTime;

    @Column(name = "friday_end_time")
    private String fridayEndTime;

    @Column(name = "saturday_start_time")
    private String saturdayStartTime;

    @Column(name = "saturday_end_time")
    private String saturdayEndTime;

    @Column(name = "sunday_start_time")
    private String sundayStartTime;

    @Column(name = "sunday_end_time")
    private String sundayEndTime;

    @Column(name = "wages_per_hour")
    private float wagesPerHour;

    @Column(name = "wages_currency")
    @Enumerated(EnumType.STRING)
    private Currency wagesCurrency;

    @Column(name = "is_expired")
    private boolean isExpired;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}