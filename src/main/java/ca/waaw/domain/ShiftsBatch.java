package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "shifts_batch")
@EqualsAndHashCode(callSuper = true)
public class ShiftsBatch extends AbstractEntity {

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "name")
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Location location;

    @Column(name = "location_id")
    private String locationId;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private LocationRole locationRole;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "batch_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<ShiftBatchUserMapping> mappedUsers;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "is_released")
    private boolean isReleased;

    public List<String> getUsers() {
        if (mappedUsers != null)
            return this.mappedUsers.stream().map(ShiftBatchUserMapping::getUserId).collect(Collectors.toList());
        else return null;
    }

}