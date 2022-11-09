package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.TimeOffStatus;
import ca.waaw.enumration.TimeOffType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "time_offs")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "DetailedTimeOff.getByUserIdAfterDate", query = "SELECT dt FROM DetailedTimeOff dt " +
                "WHERE dt.userDetails.id = ?1 AND dt.createdDate > ?2 AND (?3 IS NULL OR dt.status = ?3) " +
                "AND dt.deleteFlag = false"),
        @NamedQuery(name = "DetailedTimeOff.getByLocationIdAfterDate", query = "SELECT dt FROM DetailedTimeOff " +
                "dt WHERE dt.userDetails.locationId = ?1 AND dt.createdDate > ?2 AND (?3 IS NULL OR " +
                "dt.status = ?3) AND dt.deleteFlag = false"),
        @NamedQuery(name = "DetailedTimeOff.getByOrganizationIdAfterDate", query = "SELECT dt FROM DetailedTimeOff " +
                "dt WHERE dt.userDetails.organizationId = ?1 AND dt.createdDate > ?2 AND (?3 IS NULL OR " +
                "dt.status = ?3) AND dt.deleteFlag = false"),
        @NamedQuery(name = "DetailedTimeOff.getByUserIdBetweenDates", query = "SELECT dt FROM DetailedTimeOff dt " +
                "WHERE dt.userDetails.id = ?1 AND dt.createdDate BETWEEN ?2 AND ?3 AND (?4 IS NULL OR dt.status = ?4) " +
                "AND dt.deleteFlag = false"),
        @NamedQuery(name = "DetailedTimeOff.getByLocationIdBetweenDates", query = "SELECT dt FROM DetailedTimeOff dt " +
                "WHERE dt.userDetails.locationId = ?1 AND dt.createdDate BETWEEN ?2 AND ?3 AND (?4 IS NULL OR dt.status = ?4) " +
                "AND dt.deleteFlag = false"),
        @NamedQuery(name = "DetailedTimeOff.getByOrganizationIdBetweenDates", query = "SELECT dt FROM DetailedTimeOff " +
                "dt WHERE dt.userDetails.organizationId = ?1 AND dt.createdDate BETWEEN ?2 AND ?3 AND (?4 IS NULL " +
                "OR dt.status = ?4) AND dt.deleteFlag = false")
})
public class DetailedTimeOff extends AbstractEntity {

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    private String notes;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private UserOrganization userDetails;

    @Column
    @Enumerated(EnumType.STRING)
    private TimeOffType type;

    @Column
    @Enumerated(EnumType.STRING)
    private TimeOffStatus status;

}