package ca.waaw.domain;

import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "shifts")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQuery(name = "Shifts.getByUserIdBetweenDates", query = "SELECT s FROM Shifts s WHERE" +
        "s.userId = ?1 AND (s.start IS BETWEEN ?2 AND ?3 OR s.end IS BETWEEN ?2 AND ?3)")
public class Shifts extends AbstractEntity {

    @Column(name = "user_id")
    private String userId;

    private Instant start;

    private Instant end;

    private String notes;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column(name = "assign_to_first_claim")
    private boolean assignToFirstClaim = false;

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