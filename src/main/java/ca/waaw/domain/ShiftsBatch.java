package ca.waaw.domain;

import ca.waaw.enumration.ShiftBatchStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "shifts_batch")
@EqualsAndHashCode(callSuper = true)
public class ShiftsBatch extends AbstractEntity {

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

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "batch_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<ShiftBatchMapping> mappedUsersAndRoles;

    @Column
    @Enumerated(EnumType.STRING)
    private ShiftBatchStatus status = ShiftBatchStatus.CREATING;

    @Column(name = "is_released")
    private boolean isReleased;

}