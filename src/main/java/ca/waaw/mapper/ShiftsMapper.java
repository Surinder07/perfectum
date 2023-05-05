package ca.waaw.mapper;

import ca.waaw.domain.ShiftBatchMapping;
import ca.waaw.domain.Shifts;
import ca.waaw.domain.ShiftsBatch;
import ca.waaw.domain.joined.BatchDetails;
import ca.waaw.domain.joined.ShiftDetails;
import ca.waaw.domain.joined.ShiftDetailsWithBatch;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.enumration.ShiftBatchStatus;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShiftsMapper {


    public static Shifts shiftDtoToEntity(NewShiftDto source, String userId, String locationId, String locationRoleId,
                                          String loggedInUser, String organizationId, String timezone, String batchId,
                                          String customId) {
        Shifts target = new Shifts();
        target.setBatchId(batchId);
        target.setUserId(userId);
        target.setLocationId(locationId);
        target.setLocationRoleId(locationRoleId);
        target.setNotes(source.getNotes());
        target.setStart(DateAndTimeUtils.getDateInstant(source.getStart().getDate(), source.getStart().getTime(), timezone));
        target.setEnd(DateAndTimeUtils.getDateInstant(source.getEnd().getDate(), source.getEnd().getTime(), timezone));
        target.setCreatedBy(loggedInUser);
        target.setOrganizationId(organizationId);
        target.setShiftStatus(getNewShiftStatus(userId, source.isInstantRelease()));
        target.setShiftType(ShiftType.SINGLE);
        target.setWaawId(customId);
        return target;
    }

    /**
     * @param source   new shift batch dto
     * @param timezone timezone for location
     * @return shift batch entity to be saved in database
     */
    public static ShiftsBatch dtoToEntityBatch(NewShiftDto source, String timezone) {
        ShiftsBatch target = new ShiftsBatch();
        target.setName(source.getShiftName());
        target.setStatus(ShiftBatchStatus.CREATING);
        if (source.getUserIds() != null && source.getUserIds().size() > 0) {
            target.setMappedUsersAndRoles(source.getUserIds().stream()
                    .map(userId -> {
                        ShiftBatchMapping map = new ShiftBatchMapping();
                        map.setBatchId(target.getId());
                        map.setUserId(userId);
                        return map;
                    }).collect(Collectors.toList()));
        } else if (source.getLocationRoleIds() != null && source.getLocationRoleIds().size() > 0) {
            target.setMappedUsersAndRoles(source.getLocationRoleIds().stream()
                    .map(locationRoleId -> {
                        ShiftBatchMapping map = new ShiftBatchMapping();
                        map.setBatchId(target.getId());
                        map.setLocationRoleId(locationRoleId);
                        return map;
                    }).collect(Collectors.toList()));
        }
        target.setLocationId(source.getLocationId());
        try {
            target.setStartDate(DateAndTimeUtils.getDateAtStartOrEnd(source.getStart().getDate(), "start", timezone));
            target.setEndDate(DateAndTimeUtils.getDateAtStartOrEnd(source.getEnd().getDate(), "end", timezone));
        } catch (Exception ignored) {
        }
        return target;
    }

    /**
     * @param batchSource  Page objects for all batches
     * @param shiftsSource map of list of all shifts for each batch
     * @return dto to be returned
     */
    public static BatchDetailsDto entitiesToListingDto(BatchDetails batchSource, Map<String, List<ShiftDetails>> shiftsSource, String timezone) {
        BatchDetailsDto target = new BatchDetailsDto();
        BeanUtils.copyProperties(batchSource, target);
        target.setStartDate(DateAndTimeUtils.getFullMonthDate(batchSource.getStartDate(), timezone));
        target.setEndDate(DateAndTimeUtils.getFullMonthDate(batchSource.getEndDate(), timezone));
        target.setCreationDate(DateAndTimeUtils.getFullMonthDate(batchSource.getCreatedDate(), timezone));
        target.setStatus(batchSource.getStatus() == null ? "-" : batchSource.getStatus().toString());
        List<ShiftDetailsDto> shifts = shiftsSource.get(batchSource.getId()) == null ?
                new ArrayList<>() : shiftsSource.get(batchSource.getId())
                .stream().map(shift -> entityToShiftDto(shift, timezone)).collect(Collectors.toList());
        target.setShifts(shifts);
        return target;
    }

    /**
     * @param source   Shift details entity
     * @param timezone timezone in which dates are required
     * @return dto
     */
    public static ShiftDetailsDto entityToShiftDto(ShiftDetailsWithBatch source, String timezone) {
        ShiftDetailsDto targetShift = new ShiftDetailsDto();
        targetShift.setId(source.getId());
        targetShift.setShiftType(source.getShiftType());
        targetShift.setEmployeeName(source.getUser().getFullName());
        targetShift.setEmployeeEmail(source.getUser().getEmail());
        targetShift.setShiftStatus(source.getShiftStatus());
        targetShift.setStart(DateAndTimeUtils.getDateTimeObject(source.getStart(), timezone));
        targetShift.setEnd(DateAndTimeUtils.getDateTimeObject(source.getEnd(), timezone));
        targetShift.setNotes(source.getNotes());
        targetShift.setWaawId(source.getWaawId());
        targetShift.setName(source.getBatch().getName());
        return targetShift;
    }

    /**
     * @param source   Shift details entity
     * @param timezone timezone in which dates are required
     * @return dto
     */
    public static ShiftDetailsDto entityToShiftDto(ShiftDetails source, String timezone) {
        ShiftDetailsDto targetShift = new ShiftDetailsDto();
        targetShift.setId(source.getId());
        targetShift.setEmployeeId(source.getUser() == null ? "N/A" : source.getUser().getWaawId());
        targetShift.setEmployeeName(source.getUser() == null ? "N/A" : CommonUtils
                .combineFirstAndLastName(source.getUser().getFirstName(), source.getUser().getLastName()));
        targetShift.setEmployeeEmail(source.getUser() == null ? "N/A" : source.getUser().getEmail());
        targetShift.setLocationName(source.getLocation() == null ? "N/A" : source.getLocation().getName());
        targetShift.setLocationRoleName(source.getLocationRole() == null ? "N/A" : source.getLocationRole().getName());
        targetShift.setShiftType(source.getShiftType());
        targetShift.setShiftStatus(source.getShiftStatus());
        targetShift.setStart(DateAndTimeUtils.getDateTimeObjectWithFullDate(source.getStart(), timezone));
        targetShift.setEnd(DateAndTimeUtils.getDateTimeObjectWithFullDate(source.getEnd(), timezone));
        targetShift.setNotes(source.getNotes());
        return targetShift;
    }

    /**
     * @return Shift status based on details provided in dto
     */
    private static ShiftStatus getNewShiftStatus(String userId, boolean isInstantRelease) {
        if (StringUtils.isNotEmpty(userId) && isInstantRelease) return ShiftStatus.RELEASED;
        else if (StringUtils.isNotEmpty(userId)) return ShiftStatus.ASSIGNED;
        else return ShiftStatus.CREATED;
    }

}
