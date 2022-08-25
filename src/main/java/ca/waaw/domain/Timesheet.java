package ca.waaw.domain;

import ca.waaw.enumration.TimeSheetType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "timesheet")
public class Timesheet implements Serializable {

    @Id
    @Column(name = "uuid")
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column
    @Enumerated(EnumType.STRING)
    private TimeSheetType type;

    @Column(name = "added_by")
    private String addedBy;

    @Column(name = "del_flg")
    private boolean deleteFlag;

}