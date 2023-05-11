package ca.waaw.domain.locationandroles;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.domain.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "location_role")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationRoleUsers extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "admin_rights")
    private boolean adminRights;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToMany
    @JoinColumn(name = "location_role_id", referencedColumnName = "uuid")
    private List<User> users;

}