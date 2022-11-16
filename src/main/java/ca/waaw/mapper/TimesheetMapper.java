package ca.waaw.mapper;

import ca.waaw.domain.Timesheet;
import ca.waaw.domain.joined.DetailedTimesheet;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.TimesheetDetailDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import ca.waaw.enumration.TimeSheetType;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;

import java.time.Instant;

public class TimesheetMapper {

    /**
     * @param loggedInUser logged-in user for whom timer will be started
     * @return Timesheet entity to be saved in the database
     */
    public static Timesheet createNewEntityForLoggedInUser(UserOrganization loggedInUser) {
        Timesheet target = new Timesheet();
        target.setStart(DateAndTimeUtils.getCurrentDateTime(loggedInUser.getLocation().getTimezone()));
        target.setType(TimeSheetType.CLOCKED);
        target.setCreatedBy(loggedInUser.getId());
        target.setUserId(loggedInUser.getId());
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
     * @param source   Detailed timesheet details
     * @param timezone timezone for logged-in user
     * @return Dto for timesheet details
     */
    public static TimesheetDetailDto entityToDto(DetailedTimesheet source, String timezone) {
        TimesheetDetailDto target = new TimesheetDetailDto();
        target.setId(source.getId());
        target.setStart(DateAndTimeUtils.getDateTimeObject(source.getStart(), timezone));
        target.setEnd(DateAndTimeUtils.getDateTimeObject(source.getEnd(), timezone));
        UserInfoForDropDown user = new UserInfoForDropDown();
        user.setId(source.getUserDetails().getId());
        user.setEmail(source.getUserDetails().getEmail());
        user.setFullName(CommonUtils.combineFirstAndLastName(source.getUserDetails().getFirstName(),
                source.getUserDetails().getLastName()));
        user.setAuthority(source.getUserDetails().getAuthority());
        target.setUser(user);
        target.setLocationAndRole(LocationRoleMapper.locationEntityToDetailDto(source.getUserDetails().getLocation(),
                source.getUserDetails().getLocationRole()));
        return target;
    }

}
