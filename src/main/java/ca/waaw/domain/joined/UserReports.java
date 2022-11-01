package ca.waaw.domain.joined;

import ca.waaw.domain.*;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.Currency;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "user")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SecondaryTable(name = "employee_preferences", pkJoinColumns = @PrimaryKeyJoinColumn(name = "user_id"))
public class UserReports extends AbstractEntity {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "organization_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Location location;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private LocationRole locationRole;

    @Column
    @Enumerated(EnumType.STRING)
    private Authority authority;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<Shifts> shiftInfo;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<Timesheet> timesheetInfo;

    @Column(table = "employee_preferences", name = "wages_per_hour")
    private float wagesPerHour;

    @Enumerated(EnumType.STRING)
    @Column(table = "employee_preferences", name = "wages_currency")
    private Currency wagesCurrency;

    @Column(name = "is_full_time")
    private boolean isFullTime;

}