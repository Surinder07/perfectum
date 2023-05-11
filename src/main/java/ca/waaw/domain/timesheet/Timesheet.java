package ca.waaw.domain.timesheet;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.TimeSheetType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "time_sheets")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "Timesheet.getByUserIdBetweenDates", query = "SELECT t FROM Timesheet t WHERE " +
                "t.userId = ?1 AND (t.start BETWEEN ?2 AND ?3 OR t.end BETWEEN ?2 AND ?3) AND t.deleteFlag = FALSE"),
        @NamedQuery(name = "Timesheet.getAllByUserIdBetweenDates", query = "SELECT t FROM Timesheet t WHERE " +
                "t.userId = ?1 AND (t.start BETWEEN ?2 AND ?3 OR t.end BETWEEN ?2 AND ?3) AND t.deleteFlag = FALSE"),
        @NamedQuery(name = "Timesheet.getActiveTimesheet", query = "SELECT t FROM Timesheet t WHERE t.userId = ?1 " +
                "AND t.end IS NULL AND t.deleteFlag = FALSE")
})
public class Timesheet extends AbstractEntity {

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column(name = "shift_id")
    private String shiftId;

    @Column
    @Enumerated(EnumType.STRING)
    private TimeSheetType type;

    @Column
    private String comment;

}