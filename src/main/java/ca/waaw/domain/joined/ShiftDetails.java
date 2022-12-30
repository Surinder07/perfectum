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
@NamedQuery(name = "ShiftDetails.searchAndFilterShifts", query = "SELECT s from ShiftDetails s LEFT JOIN FETCH s.user u " +
        "LEFT JOIN FETCH s.location l LEFT JOIN FETCH s.locationRole r  " +
        "WHERE (?1 IS NULL OR (u.firstName LIKE CONCAT('%', ?1, '%') OR u.lastName LIKE CONCAT('%', ?1, '%') OR " +
        "u.email LIKE CONCAT('%', ?1, '%'))) AND (?2 IS NULL OR l.id = ?2) AND (?3 IS NULL OR " +
        "r.id = ?3) AND (?4 IS NULL OR s.shiftStatus = ?4) AND (?5 IS NULL OR u.id = ?5)" +
        "AND (?6 = TRUE OR r.adminRights = FALSE) AND s.deleteFlag = FALSE " +
        "AND ((?7 IS NULL OR ?8 IS NULL) OR (b.startDate BETWEEN ?7 AND ?8 OR b.endDate " +
        "BETWEEN ?7 AND ?8))")
public class ShiftDetails extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid")
    private User user;

    @Column(name = "batch_id")
    private String batchId;

    private Instant start;

    private Instant end;

    private String notes;

    @Column(name = "organization_id")
    private String organizationId;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid")
    private Location location;

    @OneToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid")
    private LocationRole locationRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_status")
    private ShiftStatus shiftStatus;

}