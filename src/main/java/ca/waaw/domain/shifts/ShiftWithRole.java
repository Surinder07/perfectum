package ca.waaw.domain.shifts;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
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
public class ShiftWithRole extends AbstractEntity {

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "waaw_batch_id")
    private String waawBatchId;

    @Column(name = "waaw_shift_id")
    private String waawShiftId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "batch_name")
    private String batchName;

    @Column(name = "batch_start")
    private Instant batchStart;

    @Column(name = "batch_end")
    private Instant batchEnd;

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column
    private String notes;

    @Column
    private String conflicts;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private LocationRole locationRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_status")
    private ShiftStatus shiftStatus;

}