package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.LocationRole;
import ca.waaw.domain.ShiftsBatch;
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
public class ShiftDetailsWithBatch extends AbstractEntity {

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "batch_id", referencedColumnName = "uuid")
    private ShiftsBatch batch;

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column
    private String notes;

    @Column
    private String conflicts;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private LocationRole locationRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_status")
    private ShiftStatus shiftStatus;

}