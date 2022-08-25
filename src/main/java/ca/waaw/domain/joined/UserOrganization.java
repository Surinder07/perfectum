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
@NamedQueries({
        @NamedQuery(name = "UserOrganization.searchUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority",
                query = "SELECT u FROM UserOrganization u WHERE (u.firstName LIKE ?1 OR u.lastName LIKE ?1 " +
                        "or u.email LIKE ?1 OR u.employeeId LIKE ?1 OR u.waawId LIKE ?1) " +
                        "AND u.organizationId = ?2 AND (?3 IS NULL OR u.locationId = ?3) AND u.deleteFlag " +
                        "= ?4 AND (?5 IS NULL OR u.authority = ?5)"),
        @NamedQuery(name = "UserOrganization.searchUsersWithLocationIdAndDeleteFlagAndAuthority",
                query = "SELECT u FROM UserOrganization u WHERE (u.firstName LIKE ?1 OR u.lastName LIKE ?1 " +
                        "or u.email LIKE ?1 OR u.employeeId LIKE ?1 OR u.waawId LIKE ?1) AND u.locationId = ?2 " +
                        "AND u.deleteFlag = ?3 AND (?4 IS NULL OR u.authority = ?4)"),
        @NamedQuery(name = "UserOrganization.findUsersWithLocationIdAndDeleteFlagAndAuthority",
                query = "SELECT u FROM UserOrganization u WHERE u.locationId = ?1 " +
                        "AND u.deleteFlag = ?2 AND (?3 IS NULL OR u.authority = ?3)"),
        @NamedQuery(name = "UserOrganization.findUsersWithOrganizationIdAndLocationIdAndDeleteFlagAndAuthority",
                query = "SELECT u FROM UserOrganization u WHERE u.organizationId = ?1 AND (?2 IS NULL OR " +
                        "u.locationId = ?2) AND u.deleteFlag = ?3 AND (?4 IS NULL OR u.authority = ?4)")
})
public class UserOrganization extends AbstractEntity {

    @Column
    private String username;

    @Column(name = "first_name")
    private String firstName;

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

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column
    @Enumerated(EnumType.STRING)
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
