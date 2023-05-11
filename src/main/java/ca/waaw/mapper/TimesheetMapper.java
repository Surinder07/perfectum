package ca.waaw.mapper;

import ca.waaw.domain.timesheet.Timesheet;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.timesheet.TimesheetDetailsDto;
import ca.waaw.enumration.TimeSheetType;
import ca.waaw.web.rest.utils.DateAndTimeUtils;

import java.time.Instant;

public class TimesheetMapper {

    /**
     * @param loggedInUser logged-in user for whom timer will be started
     * @return Timesheet entity to be saved in the database
     */
    public static Timesheet createNewEntityForLoggedInUser(UserOrganization loggedInUser, String shiftId) {
        Timesheet target = new Timesheet();
        target.setStart(Instant.now());
        target.setOrganizationId(loggedInUser.getOrganizationId());
        target.setLocationId(loggedInUser.getLocationId());
        target.setLocationRoleId(loggedInUser.getLocationRoleId());
        target.setType(TimeSheetType.CLOCKED);
        target.setCreatedBy(loggedInUser.getId());
        target.setUserId(loggedInUser.getId());
        target.setShiftId(shiftId);
        return target;
    }

    /**
     * @param loggedUserId id for logged-in user
     * @param start        Instant object for start time
     * @param end          Instant object for end time
     * @return Timesheet object to save in database
     */
    public static Timesheet dtoToEntity(String loggedUserId, Instant start, Instant end) {
        Timesheet target = new Timesheet();
        target.setStart(start);
        target.setEnd(end);
        target.setType(TimeSheetType.ADDED_BY_ADMIN);
        target.setCreatedBy(loggedUserId);
        return target;
    }

    /**
     * @param source   source entity for timesheet
     * @param timezone timezone for logged-in user
     * @return dto populated with timesheet details
     */
    public static TimesheetDetailsDto entityToDetailedDto(Timesheet source, String timezone) {
        TimesheetDetailsDto target = new TimesheetDetailsDto();
        target.setId(source.getId());
        target.setType(source.getType().equals(TimeSheetType.ADDED_BY_ADMIN) ? "Manual" : "Clocked");
        target.setStart(DateAndTimeUtils.getDateTimeObject(source.getStart(), timezone));
        target.setEnd(source.getEnd() != null ? DateAndTimeUtils.getDateTimeObject(source.getEnd(), timezone) : null);
        target.setDuration(source.getEnd() != null ? DateAndTimeUtils.getTimeBetweenInstants(source.getStart(), source.getEnd()) : null);
        target.setComment(source.getComment());
        return target;
    }

}
