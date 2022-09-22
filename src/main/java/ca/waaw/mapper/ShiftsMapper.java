package ca.waaw.mapper;

import ca.waaw.domain.Shifts;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class ShiftsMapper {

    /**
     * @param source                        New Shift info
     * @param locationAndRoleIdsAndTimeZone {@link String[]} with locationId at index 0, locationRoleId at index 1,
     *                                      and location timezone at index 2
     * @param loggedInUser                  logged-in user's id
     * @param organizationId                id for new shift's organization
     * @return Entity to be saved in database
     */
    public static Shifts shiftDtoToEntity(NewShiftDto source, String[] locationAndRoleIdsAndTimeZone, String loggedInUser,
                                          String organizationId) {
        Shifts target = new Shifts();
        target.setId(UUID.randomUUID().toString());
        target.setUserId(source.getUserId());
        target.setStatus(EntityStatus.ACTIVE);
        target.setLocationId(locationAndRoleIdsAndTimeZone[0]);
        target.setLocationRoleId(locationAndRoleIdsAndTimeZone[1]);
        target.setAssignToFirstClaim(source.isAssignToFirstClaim());
        target.setNotes(source.getNotes());
        target.setStart(DateAndTimeUtils.getDateInstant(source.getStart().getDate(), source.getStart().getTime(),
                locationAndRoleIdsAndTimeZone[2]));
        target.setEnd(DateAndTimeUtils.getDateInstant(source.getEnd().getDate(), source.getEnd().getTime(),
                locationAndRoleIdsAndTimeZone[2]));
        target.setCreatedBy(loggedInUser);
        target.setOrganizationId(organizationId);
        target.setShiftStatus(getNewShiftStatus(source));
        target.setShiftType(ShiftType.SINGLE);
        return target;
    }

    private static ShiftStatus getNewShiftStatus(NewShiftDto dto) {
        if (StringUtils.isNotEmpty(dto.getUserId()) && dto.isInstantRelease()) return ShiftStatus.SCHEDULED;
        else if (StringUtils.isNotEmpty(dto.getUserId())) return ShiftStatus.CREATED_ASSIGNED;
        else if (dto.isInstantRelease()) return ShiftStatus.RELEASED_UNASSIGNED;
        else return ShiftStatus.CREATED_UNASSIGNED;
    }

}
