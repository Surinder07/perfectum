package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "time_offs")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQuery(name = "TimeOffs.getByUserIdBetweenDates", query = "SELECT to FROM TimeOffs to WHERE " +
        "to.userId = ?1 AND (to.startDate BETWEEN ?2 AND ?3 OR to.endDate BETWEEN ?2 AND ?3) AND to.deleteFlag = FALSE")
public class TimeOffs extends AbstractEntity {

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    private String notes;

    @Column(name = "user_id")
    private String userId;

}