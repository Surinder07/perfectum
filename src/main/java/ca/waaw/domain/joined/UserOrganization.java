package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
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

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "lang_key")
    private String langKey;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "organization", referencedColumnName = "uuid")
    private Organization organization;

    @Column
    private Authority authority;

    @Column(name = "activated")
    private Boolean isActivated;

    @Column(name = "suspended")
    private Boolean isSuspended;

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

    @Column(name = "last_login")
    private Instant lastLogin;

}
