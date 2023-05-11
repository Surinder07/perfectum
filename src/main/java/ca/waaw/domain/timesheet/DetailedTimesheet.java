package ca.waaw.domain.timesheet;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.user.User;
import ca.waaw.enumration.TimeSheetType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "time_sheets")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "DetailedTimesheet.getByOrganizationIdAndDates", query = "SELECT dt FROM DetailedTimesheet dt " +
                "WHERE dt.userDetails.organizationId = ?1 AND (dt.start BETWEEN ?2 AND ?3) AND dt.deleteFlag = FALSE"),
        @NamedQuery(name = "DetailedTimesheet.getByLocationIdAndDates", query = "SELECT dt FROM DetailedTimesheet dt " +
                "WHERE dt.userDetails.locationId = ?1 AND (dt.start BETWEEN ?2 AND ?3) AND dt.deleteFlag = FALSE"),
        @NamedQuery(name = "DetailedTimesheet.getByUserIdAndDates", query = "SELECT dt FROM DetailedTimesheet dt " +
                "WHERE dt.userDetails.id = ?1 AND (dt.start BETWEEN ?2 AND ?3) AND dt.deleteFlag = FALSE")
})
public class DetailedTimesheet extends AbstractEntity {

    private Instant start;

    private Instant end;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "organization_id")
    private String organizationId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", insertable = false, updatable = false)
    private User userDetails;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", insertable = false, updatable = false)
    private Location location;

    @Column(name = "location_id")
    private String locationId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", insertable = false, updatable = false)
    private LocationRole locationRole;

    @Column
    @Enumerated(EnumType.STRING)
    private TimeSheetType type;

}