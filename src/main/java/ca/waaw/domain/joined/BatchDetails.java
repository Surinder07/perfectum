package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.ShiftBatchStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "shifts_batch")
@EqualsAndHashCode(callSuper = true)
@NamedQuery(name = "BatchDetails.searchAndFilterShifts", query = "SELECT b from BatchDetails b " +
        "WHERE (?1 IS NULL OR (b.name LIKE CONCAT('%', ?1, '%') OR b.waawId LIKE CONCAT('%', ?1, '%'))) " +
        "AND (?2 IS NULL OR b.organizationId = ?2) AND (?3 IS NULL OR b.locationId = ?3) AND ((?4 IS NULL OR ?5 IS NULL) " +
        "OR (b.startDate BETWEEN ?4 AND ?5 OR b.endDate BETWEEN ?4 AND ?5)) AND (?6 IS NULL OR b.status = ?6) " +
        "AND b.deleteFlag = FALSE ORDER BY b.createdDate DESC")
public class BatchDetails extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column
    @Enumerated(EnumType.STRING)
    private ShiftBatchStatus status;

    @Column(name = "is_released")
    private boolean isReleased;

}