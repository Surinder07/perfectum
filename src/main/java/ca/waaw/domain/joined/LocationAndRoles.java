package ca.waaw.domain.joined;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.LocationRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "location")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationAndRoles extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @Column
    private String timezone;

    @OneToMany
    @JoinColumn(name = "location_id", referencedColumnName = "uuid")
    private List<LocationRole> locationRoles;

}