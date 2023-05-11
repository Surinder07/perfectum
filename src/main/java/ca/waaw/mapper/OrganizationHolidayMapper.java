package ca.waaw.mapper;

import ca.waaw.domain.organization.OrganizationHolidays;
import ca.waaw.dto.holiday.HolidayDto;
import ca.waaw.enumration.HolidayType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

public class OrganizationHolidayMapper {

    /**
     * @param source New Holiday info DTO
     * @return Holiday entity to be saved in database
     */
    public static OrganizationHolidays newDtoToEntity(HolidayDto source) {
        OrganizationHolidays target = new OrganizationHolidays();
        BeanUtils.copyProperties(source, target);
        if (StringUtils.isEmpty(target.getLocationId())) target.setLocationId(null);
        target.setType(HolidayType.valueOf(source.getType()));
        return target;
    }

    /**
     * @param source Holiday info DTO to be updated
     * @param target Existing Holiday entity to be updated
     */
    public static void updateDtoToEntity(HolidayDto source, OrganizationHolidays target) {
        if (StringUtils.isNotEmpty(source.getName())) target.setName(source.getName());
        if (StringUtils.isNotEmpty(source.getLocationId())) target.setLocationId(source.getLocationId());
        if (StringUtils.isNotEmpty(source.getType())) target.setType(HolidayType.valueOf(source.getType()));
        if (source.getYear() > 2021) target.setYear(source.getYear());
        if (source.getMonth() > 0 && source.getMonth() < 13) target.setMonth(source.getMonth());
        if (source.getDate() > 0 && source.getDate() < 32) target.setDate(source.getDate());
    }

    /**
     * @param source Existing Holiday entity
     * @return Holiday info DTO
     */
    public static HolidayDto entityToDto(OrganizationHolidays source) {
        HolidayDto target = new HolidayDto();
        BeanUtils.copyProperties(source, target);
        target.setType(source.getType().toString());
        return target;
    }

}
