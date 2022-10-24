package ca.waaw.domain;

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
        @NamedQuery(name = "Timesheet.getActiveTimesheet", query = "SELECT t FROM Timesheet t WHERE t.userId = ?1 " +
                "AND t.end IS NULL AND t.deleteFlag = FALSE")
})
public class Timesheet extends AbstractEntity {

    private Instant start;

    private Instant end;

    @Column(name = "user_id")
    private String userId;

    private TimeSheetType type;

}