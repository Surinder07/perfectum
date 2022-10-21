package ca.waaw.domain;

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
public class Timesheet extends AbstractEntity {

    private Instant start;

    private Instant end;

    @Column(name = "user_id")
    private String userId;

}