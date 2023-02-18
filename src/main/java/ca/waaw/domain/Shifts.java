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
@NamedQuery(name = "Shifts.getByUserIdBetweenDates", query = "SELECT s FROM Shifts s WHERE " +
        "s.userId = ?1 AND (s.start BETWEEN ?2 AND ?3 OR s.end BETWEEN ?2 AND ?3) AND s.deleteFlag = FALSE")
public class Shifts extends AbstractEntity {

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "batch_id")
    private String batchId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_status")
    private ShiftStatus shiftStatus;

}