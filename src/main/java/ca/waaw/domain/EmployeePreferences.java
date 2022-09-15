package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "employee_preferences")
public class EmployeePreferences {

    @Id
    @Column(name = "uuid")
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "monday_start_time")
    private String mondayStartTime;

    @Column(name = "monday_working_hours")
    private float mondayWorkingHours;

    @Column(name = "tuesday_start_time")
    private String tuesdayStartTime;

    @Column(name = "tuesday_working_hours")
    private float tuesdayWorkingHours;

    @Column(name = "wednesday_start_time")
    private String wednesdayStartTime;

    @Column(name = "wednesday_working_hours")
    private float wednesdayWorkingHours;

    @Column(name = "thursday_start_time")
    private String thursdayStartTime;

    @Column(name = "thursday_working_hours")
    private float thursdayWorkingHours;

    @Column(name = "friday_start_time")
    private String fridayStartTime;

    @Column(name = "friday_working_hours")
    private float fridayWorkingHours;

    @Column(name = "saturday_start_time")
    private String saturdayStartTime;

    @Column(name = "saturday_working_hours")
    private float saturdayWorkingHours;

    @Column(name = "sunday_start_time")
    private String sundayStartTime;

    @Column(name = "sunday_working_hours")
    private float sundayWorkingHours;

    @Column(name = "is_expired")
    private boolean isExpired;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}