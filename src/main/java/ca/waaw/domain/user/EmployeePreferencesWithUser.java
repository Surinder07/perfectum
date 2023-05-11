package ca.waaw.domain.user;

import ca.waaw.enumration.Currency;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "user")
@SecondaryTable(name = "employee_preferences", pkJoinColumns = @PrimaryKeyJoinColumn(name = "user_id"))
public class EmployeePreferencesWithUser {

    @Id
    @Column(name = "uuid", insertable = false, updatable = false)
    private String userId;

    @Column(name = "first_name", insertable = false, updatable = false)
    private String firstName;

    @Column(name = "last_name", insertable = false, updatable = false)
    private String lastName;

    @Column(insertable = false, updatable = false)
    private String email;

    @Column(insertable = false, updatable = false)
    private String username;

    @Column(name = "lang_key", insertable = false, updatable = false)
    private String langKey;

    @Column(name = "employee_id", insertable = false, updatable = false)
    private String employeeId;

    @Column(name = "waaw_custom_id", insertable = false, updatable = false)
    private String waawId;

    @Column(name = "is_full_time", insertable = false, updatable = false)
    private boolean isFullTime;

    @Column(table = "employee_preferences", name = "uuid")
    private String id;

    @Column(table = "employee_preferences", name = "monday_start_time")
    private String mondayStartTime;

    @Column(table = "employee_preferences", name = "monday_end_time")
    private String mondayEndTime;

    @Column(table = "employee_preferences", name = "tuesday_start_time")
    private String tuesdayStartTime;

    @Column(table = "employee_preferences", name = "tuesday_end_time")
    private String tuesdayEndTime;

    @Column(table = "employee_preferences", name = "wednesday_start_time")
    private String wednesdayStartTime;

    @Column(table = "employee_preferences", name = "wednesday_end_time")
    private String wednesdayEndTime;

    @Column(table = "employee_preferences", name = "thursday_start_time")
    private String thursdayStartTime;

    @Column(table = "employee_preferences", name = "thursday_end_time")
    private String thursdayEndTime;

    @Column(table = "employee_preferences", name = "friday_start_time")
    private String fridayStartTime;

    @Column(table = "employee_preferences", name = "friday_end_time")
    private String fridayEndTime;

    @Column(table = "employee_preferences", name = "saturday_start_time")
    private String saturdayStartTime;

    @Column(table = "employee_preferences", name = "saturday_end_time")
    private String saturdayEndTime;

    @Column(table = "employee_preferences", name = "sunday_start_time")
    private String sundayStartTime;

    @Column(table = "employee_preferences", name = "sunday_end_time")
    private String sundayEndTime;

    @Column(table = "employee_preferences", name = "wages_per_hour")
    private float wagesPerHour;

    @Column(table = "employee_preferences", name = "wages_currency")
    @Enumerated(EnumType.STRING)
    private Currency wagesCurrency;

    @Column(table = "employee_preferences", name = "is_expired")
    private boolean isExpired;

    @Column(table = "employee_preferences", name = "created_by")
    private String createdBy;

    @Column(table = "employee_preferences", name = "created_date")
    private Instant createdDate = Instant.now();

    @Column(name = "location_id", updatable = false, insertable = false)
    private String locationId;

    @Column(name = "location_role_id", updatable = false, insertable = false)
    private String locationRoleId;

    @Column(name = "organization_id", updatable = false, insertable = false)
    private String organizationId;

    @Column(name = "del_flg", updatable = false, insertable = false)
    private boolean deleteFlag;

    public String getFullName() {
        return firstName + (StringUtils.isNotEmpty(lastName) ? " " + lastName : "");
    }

}