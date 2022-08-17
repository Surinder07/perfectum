package ca.waaw.domain;

import ca.waaw.enumration.NotificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "uuid")
    private String id;

    @Column
    private String title;

    @Column
    private String description;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "is_read")
    private boolean isRead;

    @Column
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "created_time")
    private Instant createdTime;

}
