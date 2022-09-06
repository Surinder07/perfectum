package ca.waaw.mapper;

import ca.waaw.domain.Shifts;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.web.rest.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class ShiftsMapper {

    /**
     * @param source                        New Shift info
     * @param locationAndRoleIdsAndTimeZone {@link String[]} with locationId at index 0, locationRoleId at index 1,
     *                                      and location timezone at index 2
     * @param loggedInUser                  logged-in user's id
     * @return Entity to be saved in database
     */
    public static Shifts shiftDtoToEntity(NewShiftDto source, String[] locationAndRoleIdsAndTimeZone, String loggedInUser) {
        Shifts target = new Shifts();
        target.setId(UUID.randomUUID().toString());
        if (StringUtils.isNotEmpty(source.getUserId())) {
            target.setUserId(source.getUserId());
            target.setStatus(EntityStatus.ACTIVE);
        } else {
            target.setStatus(EntityStatus.PENDING);
        }
        target.setLocationId(locationAndRoleIdsAndTimeZone[0]);
        target.setLocationRoleId(locationAndRoleIdsAndTimeZone[1]);
        // TODO
//        target.setAssignToFirstClaim(source.isAssignToFirstClaim());
//        target.setNotes(source.getNotes());
//        target.setStart(DateUtils.getDateInstant(source.getStart().getDate(), source.getStart().getTime(),
//                locationAndRoleIdsAndTimeZone[2]));
//        target.setEnd(DateUtils.getDateInstant(source.getEnd().getDate(), source.getEnd().getTime(),
//                locationAndRoleIdsAndTimeZone[2]));
        target.setCreatedBy(loggedInUser);
        return target;
    }

}
