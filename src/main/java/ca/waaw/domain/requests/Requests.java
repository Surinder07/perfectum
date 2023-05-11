package ca.waaw.domain.requests;

import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.request.RequestType;
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
@Table(name = "requests")
public class Requests implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestType type;

    @Column(name = "sub_type")
    @Enumerated(EnumType.STRING)
    private RequestSubType subType;

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column
    private String description;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.NEW;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}