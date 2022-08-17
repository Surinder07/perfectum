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
public class Location extends AbstractEntity {

    @Column
    private String name;

    @Column(name = "organization_id")
    private String organizationId;

    @Column
    private String timezone;

}