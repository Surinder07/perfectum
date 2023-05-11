package ca.waaw.domain.requests;

import ca.waaw.enumration.request.RequestStatus;
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
@Table(name = "requests_history")
public class RequestsHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "request_id")
    private String requestId;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type")
    private RequestStatus commentType;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column(name = "commenter_name")
    private String commenterName;

    @Column(name = "commenter_id")
    private String commenterId;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}