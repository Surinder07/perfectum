package ca.waaw.mapper;

import ca.waaw.domain.shifts.ShiftDetails;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.sqldtos.ShiftBatchSqlDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShiftsMapper {

    public static Shifts shiftDtoToEntity(NewShiftDto source, UserOrganization user, String batchId, String customBatchId,
                                          String shiftId, UserOrganization loggedUser) {
        String timezone = loggedUser.getAuthority().equals(Authority.ADMIN) ? loggedUser.getOrganization().getTimezone() :
                loggedUser.getLocation().getTimezone();
        Shifts target = new Shifts();
        target.setBatchId(batchId);
        target.setWaawBatchId(customBatchId);
        target.setWaawShiftId(shiftId);
        target.setBatchName(source.getShiftName());
        target.setUserId(user.getId());
        target.setOrganizationId(user.getOrganizationId());
        target.setLocationId(user.getLocationId());
        target.setLocationRoleId(user.getLocationRoleId());
        target.setNotes(source.getNotes());
        target.setStart(DateAndTimeUtils.getDateInstant(source.getStart().getDate(), source.getStart().getTime(), timezone));
        target.setEnd(DateAndTimeUtils.getDateInstant(source.getEnd().getDate(), source.getEnd().getTime(), timezone));
        target.setBatchStart(target.getStart());
        target.setBatchEnd(target.getEnd());
        target.setCreatedBy(loggedUser.getId());
        target.setShiftStatus(getNewShiftStatus(user.getId(), source.isInstantRelease()));
        target.setShiftType(ShiftType.SINGLE);
        return target;
    }

    /**
     * @return Shift status based on details provided in dto
     */
    private static ShiftStatus getNewShiftStatus(String userId, boolean isInstantRelease) {
        if (StringUtils.isNotEmpty(userId) && isInstantRelease) return ShiftStatus.RELEASED;
        else if (StringUtils.isNotEmpty(userId)) return ShiftStatus.ASSIGNED;
        else return ShiftStatus.CREATED;
    }

    /**
     * @param batchSource  Page objects for all batches
     * @param shiftsSource map of list of all shifts for each batch
     * @return dto to be returned
     */
    public static BatchDetailsDto entitiesToBatchListingDto(
            ShiftBatchSqlDto batchSource, Map<String, List<ShiftDetails>> shiftsSource, String timezone
    ) {
        BatchDetailsDto target = new BatchDetailsDto();
        target.setId(batchSource.getId());
        target.setWaawId(batchSource.getBatchId());
        target.setName(batchSource.getName());
        target.setStartDate(DateAndTimeUtils.getFullMonthDate(batchSource.getStart(), timezone));
        target.setEndDate(DateAndTimeUtils.getFullMonthDate(batchSource.getEnd(), timezone));
        target.setCreationDate(DateAndTimeUtils.getFullMonthDate(batchSource.getCreatedDate(), timezone));
        target.setStatus(getBatchStatus(shiftsSource.get(batchSource.getId())));
        List<ShiftDetailsDto> shifts = shiftsSource.get(batchSource.getId()) == null ?
                new ArrayList<>() : shiftsSource.get(batchSource.getId())
                .stream().map(shift -> entityToShiftDto(shift, timezone)).collect(Collectors.toList());
        target.setShifts(shifts);
        return target;
    }

    /**
     * @param shiftsSource List of all shifts under a batch
     * @return Status of the batch
     */
    private static String getBatchStatus(List<ShiftDetails> shiftsSource) {
        if (shiftsSource.stream().filter(shift -> shift.getShiftStatus().equals(ShiftStatus.RELEASED)).count() == shiftsSource.size()) {
            return "RELEASED";
        } else if (shiftsSource.stream().filter(shift -> shift.getShiftStatus().equals(ShiftStatus.FAILED)).count() == shiftsSource.size()) {
            return "FAILED";
        } else return "CREATED";
    }

    /**
     * @param source   Shift details entity
     * @param timezone timezone in which dates are required
     * @return dto
     */
    public static ShiftDetailsDto entityToShiftDto(ShiftDetails source, String timezone) {
        ShiftDetailsDto targetShift = new ShiftDetailsDto();
        targetShift.setId(source.getId());
        targetShift.setName(source.getBatchName());
        targetShift.setWaawId(source.getWaawShiftId());
        targetShift.setEmployeeId(source.getUser() == null ? "N/A" : source.getUser().getWaawId());
        targetShift.setEmployeeName(source.getUser() == null ? "N/A" : source.getUser().getFullName());
        targetShift.setEmployeeEmail(source.getUser() == null ? "N/A" : source.getUser().getEmail());
        targetShift.setLocationName(source.getLocation() == null ? "N/A" : source.getLocation().getName());
        targetShift.setLocationRoleName(source.getLocationRole() == null ? "N/A" : source.getLocationRole().getName());
        targetShift.setShiftType(source.getShiftType());
        targetShift.setShiftStatus(source.getShiftStatus());
        targetShift.setStart(DateAndTimeUtils.getDateTimeObjectWithFullDate(source.getStart(), timezone));
        targetShift.setEnd(DateAndTimeUtils.getDateTimeObjectWithFullDate(source.getEnd(), timezone));
        targetShift.setComments(source.getNotes());
        targetShift.setFailureReason(source.getFailureReason());
        targetShift.setConflicts((CommonUtils.commaSeparatedStringToList(source.getConflicts())));
        return targetShift;
    }

}
