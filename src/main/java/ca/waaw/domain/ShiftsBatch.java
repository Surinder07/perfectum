package ca.waaw.domain;

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
import java.util.stream.Collectors;

@Data
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "shifts_batch")
@NamedQuery(name = "ShiftsBatch.getOverlappingBatchForLocationId", query = "SELECT b FROM ShiftsBatch b " +
        "WHERE b.locationId = ?1 AND (b.startDate BETWEEN ?2 AND ?3 OR b.endDate BETWEEN ?2 AND ?3)")
public class ShiftsBatch implements Serializable {

    @Id
    @Column(name = "uuid")
    private String id = UUID.randomUUID().toString();

    @Column(name = "name")
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_role_id")
    private String locationRoleId;

    @OneToMany
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "batch_id", referencedColumnName = "uuid", updatable = false, insertable = false)
    private List<ShiftBatchUserMapping> mappedUsers;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "del_flg")
    private boolean deleteFlag;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

    public List<String> getUsers() {
        if (mappedUsers != null)
            return this.mappedUsers.stream().map(ShiftBatchUserMapping::getUserId).collect(Collectors.toList());
        else return null;
    }

}