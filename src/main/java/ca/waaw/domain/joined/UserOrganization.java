package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.Organization;
import ca.waaw.enumration.Authority;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "user")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserOrganization extends AbstractEntity {

    @Column
    private String username;

    @Column(name = "first_name")
    private String firstname;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "country_code")
    private String countryCode;

    @Column
    private String mobile;

    @Column(name = "lang_key")
    private String langKey;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "organization_id", referencedColumnName = "uuid", updatable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false)
    private Location location;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false)
    private LocationRole locationRole;

    @Column
    private Authority authority;

    @Column(name = "email_notification_on")
    private Boolean isEmailNotifications;

    @Column(name = "sms_notification_on")
    private Boolean isSmsNotifications;

    @Column(name = "reset_key")
    private String resetKey;

    @Column(name = "reset_date")
    private Instant resetDate;

    @Column(name = "activation_key")
    private String activationKey;

    @Column(name = "invite_key")
    private String inviteKey;

    @Column(name = "invited_by")
    private String invitedBy;

    @Column(name = "last_login")
    private Instant lastLogin;

}
