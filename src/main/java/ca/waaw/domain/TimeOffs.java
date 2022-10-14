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
@NamedQuery(name = "TimeOffs.getByUserIdBetweenDates", query = "SELECT to FROM TimeOffs to WHERE" +
        "to.userId = ?1 AND (to.startDate IS BETWEEN ?2 AND ?3 OR to.endDate IS BETWEEN ?2 AND ?3)")
public class TimeOffs extends AbstractEntity {

    private Instant startDate;

    private Instant endDate;

    private String note;

    private String userId;

}