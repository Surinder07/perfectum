package ca.waaw.domain;

import ca.waaw.enumration.Authority;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "user")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class User extends AbstractEntity {

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

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "waaw_custom_id")
    private String waawId;

    @Column(name = "lang_key")
    private String langKey;

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
    private boolean isEmailNotifications;

    @Column(name = "sms_notification_on")
    private boolean isSmsNotifications;

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
