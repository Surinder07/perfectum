package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.Location;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.User;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "shifts")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DetailedShift extends AbstractEntity {

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid")
    private User user;

    private Instant start;

    private Instant end;

    private String notes;

    @Column(name = "organization_id")
    private String organizationId;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid")
    private Location location;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid")
    private LocationRole locationRole;

    @Column(name = "is_conflict")
    private boolean isConflict;

    @Column(name = "conflict_reason")
    private String conflictReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_status")
    private ShiftStatus shiftStatus;

}