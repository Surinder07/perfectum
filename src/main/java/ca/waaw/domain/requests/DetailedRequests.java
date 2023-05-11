package ca.waaw.domain.requests;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.locationandroles.LocationRole;
import ca.waaw.domain.user.User;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.request.RequestType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "requests")
public class DetailedRequests implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @NotFound(action = NotFoundAction.IGNORE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private User user;

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "organization_id")
    private String organizationId;

    @NotFound(action = NotFoundAction.IGNORE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Location location;

    @NotFound(action = NotFoundAction.IGNORE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private LocationRole locationRole;

    @Column
    private Instant start;

    @Column
    private Instant end;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestType type;

    @Column(name = "sub_type")
    @Enumerated(EnumType.STRING)
    private RequestSubType subType;

    @Column
    private String description;

    @OneToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "assigned_to", referencedColumnName = "uuid", updatable = false, insertable = false)
    private User assignedTo;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column(name = "created_date")
    private Instant createdDate;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "request_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<RequestsHistory> history;

}