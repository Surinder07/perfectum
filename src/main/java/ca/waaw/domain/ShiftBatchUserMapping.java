package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "shifts_batch_user_mapping")
public class ShiftBatchUserMapping implements Serializable {

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "user_id")
    private String userId;

}