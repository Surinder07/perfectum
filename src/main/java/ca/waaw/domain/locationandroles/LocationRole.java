package ca.waaw.domain.locationandroles;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;

@Data
@Entity
@Table(name = "location_role")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "LocationRole.getListByNameAndLocation", query = "SELECT lr FROM LocationRole lr " +
                "WHERE LOWER(lr.name) IN ?1 AND lr.locationId IN ?2 AND lr.deleteFlag = false"),
        @NamedQuery(name = "LocationRole.getByNameAndLocationId", query = "SELECT lr FROM LocationRole lr " +
                "WHERE LOWER(lr.name) = ?1 AND lr.locationId = ?2 AND lr.deleteFlag = false")
})
public class LocationRole extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Location location;

    @Column(name = "total_minutes_per_day_min")
    private int totalMinutesPerDayMin = 240;

    @Column(name = "total_minutes_per_day_max")
    private int totalMinutesPerDayMax = 480;

    @Column(name = "min_minutes_between_shifts")
    private int minMinutesBetweenShifts = 720;

    @Column(name = "max_consecutive_work_days")
    private int maxConsecutiveWorkDays = 6;

    @Column(name = "admin_rights")
    private boolean adminRights;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "created_by", referencedColumnName = "uuid", updatable = false, insertable = false)
    private User createdByUser;

}