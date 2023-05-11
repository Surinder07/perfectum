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
@Table(name = "location")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocationUsers extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "waaw_id")
    private String waawId;

    @Column(name = "organization_id")
    private String organizationId;

    @Column
    private String timezone;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToMany
    @JoinColumn(name = "location_id", referencedColumnName = "uuid")
    private List<User> users;

}