package ca.waaw.domain.user;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "user")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQuery(name = "User.searchAndFilterUsers", query = "SELECT u FROM User u WHERE (?1 IS NULL OR (u.firstName LIKE " +
        "CONCAT('%', ?1, '%') OR u.lastName LIKE CONCAT('%', ?1, '%') OR u.waawId LIKE CONCAT('%', ?1, '%') " +
        "OR u.email LIKE CONCAT('%', ?1, '%'))) AND (?2 IS NULL OR u.organizationId = ?2) AND (?3 IS NULL " +
        "OR u.locationId = ?3) AND (?4 IS NULL OR u.locationRoleId = ?4) AND (?5 IS NULL OR " +
        "u.isFullTime = ?5) AND (?6 IS NULL OR u.accountStatus = ?6) AND u.deleteFlag = FALSE")
public class User extends AbstractEntity {

    @Column
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_to_update")
    private String emailToUpdate;

    @Column(name = "country")
    private String country;

    @Column(name = "country_code")
    private String countryCode;

    @Column
    private String mobile;

    @Column(name = "image_file")
    private String imageFile;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @Column(name = "stripe_id")
    private String stripeId;

    @Column(name = "lang_key")
    private String langKey = "en";

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column(name = "is_full_time")
    private boolean isFullTime;

    @Column
    @Enumerated(EnumType.STRING)
    private Authority authority;

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @Column(name = "email_notification_on")
    private boolean isEmailNotifications = true;

    @Column(name = "sms_notification_on")
    private boolean isSmsNotifications = true;

    @Column(name = "last_login")
    private Instant lastLogin;

    public String getFullName() {
        return firstName + (StringUtils.isNotEmpty(lastName) ? " " + lastName : "");
    }

}
