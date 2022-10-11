package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@Table(name = "time_offs")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TimeOffs extends AbstractEntity {

    // startDate

    // endDate

    // notes

    // startTime

    // endTime

    // userId

    // timeOffType

}