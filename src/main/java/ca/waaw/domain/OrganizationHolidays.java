package ca.waaw.domain;

import ca.waaw.enumration.HolidayType;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "organization_holidays")
@NamedQueries({
        @NamedQuery(name = "OrganizationHolidays.getAllForLocationAndMonthIfNeeded",
                query = "SELECT h FROM OrganizationHolidays h WHERE (h.locationId = ?1 OR h.locationId IS NULL) AND " +
                        "h.deleteFlag = false AND (?2 IS NULL OR h.month = ?2) AND h.year = ?3"),
        @NamedQuery(name = "OrganizationHolidays.getAllForOrganizationAndMonthIfNeeded",
                query = "SELECT h FROM OrganizationHolidays h WHERE h.organizationId = ?1 AND " +
                        "h.deleteFlag = false AND (?2 IS NULL OR h.month = ?2) AND h.year = ?3")
})
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
    private Integer year;

    @Column
    private Integer month;

    @Column
    private Integer date;

    @Column(name = "del_flg")
    private boolean deleteFlag = false;

}
