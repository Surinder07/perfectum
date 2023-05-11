package ca.waaw.domain.organization;

import ca.waaw.domain.AbstractEntity;
import ca.waaw.enumration.HolidayType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Table(name = "organization_holidays")
@NamedQueries({
        @NamedQuery(name = "OrganizationHolidays.getAllForLocationAndMonthIfNeeded",
                query = "SELECT h FROM OrganizationHolidays h WHERE (h.locationId IS NULL OR h.locationId = ?1) AND " +
                        "h.deleteFlag = false AND (?2 IS NULL OR h.month = ?2) AND h.year = ?3"),
        @NamedQuery(name = "OrganizationHolidays.getAllForOrganizationAndMonthIfNeeded",
                query = "SELECT h FROM OrganizationHolidays h WHERE h.organizationId = ?1 AND " +
                        "h.deleteFlag = false AND (?2 IS NULL OR h.month = ?2) AND h.year = ?3")
})
public class OrganizationHolidays extends AbstractEntity {

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

}
