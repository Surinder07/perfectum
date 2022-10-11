package ca.waaw.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@Table(name = "location")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "Location.getListByNameAndOrganization", query = "SELECT l FROM Location l WHERE " +
                "LOWER(l.name) IN ?1 AND l.organizationId = ?2 AND l.deleteFlag = false"),
        @NamedQuery(name = "Location.getByNameAndOrganizationId", query = "SELECT l FROM Location l WHERE " +
                "LOWER(l.name) = ?1 AND l.organizationId = ?2 AND l.deleteFlag = false")
})
public class Location extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @Column
    private String timezone;

}