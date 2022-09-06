package ca.waaw.domain;

import ca.waaw.enumration.HolidayType;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "organization_holidays")
@NamedQuery(name = "getAllForLocation",
        query ="SELECT h FROM OrganizationHoliday h WHERE (h.locationId = ?1 OR h.locationId IS NULL) AND h.deleteFlag = false")
public class OrganizationHolidays {

    @Id
    @Column(name = "uuid")
    private String id;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "location_id")
    private String locationId;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    private HolidayType type;

    @Column
    private int year;

    @Column
    private int month;

    @Column
    private int date;

    @Column(name = "del_flg")
    private boolean deleteFlag = false;

}
