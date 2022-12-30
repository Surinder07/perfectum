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
@Table(name = "shifts_batch_mapping")
public class ShiftBatchMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "location_role_id")
    private String locationRoleId;

}