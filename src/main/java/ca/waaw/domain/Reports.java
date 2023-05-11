package ca.waaw.domain;

import ca.waaw.domain.locationandroles.Location;
import ca.waaw.enumration.report.UserReport;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "reports")
public class Reports implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "waaw_id")
    private String waawId;

    @Column
    @Enumerated(EnumType.STRING)
    private UserReport type;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @OneToOne
    @NotFound(action = NotFoundAction.EXCEPTION)
    @JoinColumn(name = "location_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private Location location;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "from_date")
    private String fromDate;

    @Column(name = "to_date")
    private String toDate;

    @Column(name = "show_to_manager")
    private boolean showToManger;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

}