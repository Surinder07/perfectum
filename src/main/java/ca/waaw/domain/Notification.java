package ca.waaw.domain;

import ca.waaw.enumration.NotificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column
    private String title;

    @Column
    private String description;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "action_entity_id")
    private String actionEntityId;

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "created_time")
    private Instant createdTime = Instant.now();

}
