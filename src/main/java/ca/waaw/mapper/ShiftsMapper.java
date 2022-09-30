package ca.waaw.mapper;

import ca.waaw.domain.ShiftBatchUserMapping;
import ca.waaw.domain.Shifts;
import ca.waaw.domain.ShiftsBatch;
import ca.waaw.domain.joined.DetailedShift;
import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.stream.Collectors;

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
        if (StringUtils.isNotEmpty(source.getUserId())) target.setUserId(source.getUserId());
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

    /**
     * @param source   new shift batch dto
     * @param timezone timezone for location
     * @return shift batch entity to be saved in database
     */
    public static ShiftsBatch dtoToEntityBatch(NewShiftBatchDto source, String timezone) {
        ShiftsBatch target = new ShiftsBatch();
        if (source.getUserIds() != null && source.getUserIds().size() > 0) {
            target.setMappedUsers(source.getUserIds().stream()
                    .map(userId -> {
                        ShiftBatchUserMapping user = new ShiftBatchUserMapping();
                        user.setBatchId(target.getId());
                        user.setUserId(userId);
                        return user;
                    }).collect(Collectors.toList()));
        } else if (StringUtils.isNotEmpty(source.getLocationRoleId())) {
            target.setLocationRoleId(source.getLocationRoleId());
        }
        target.setLocationId(source.getLocationId());
        try {
            target.setStartDate(DateAndTimeUtils.getDateAtStartOrEnd(source.getStartDate(), "start", timezone));
            target.setEndDate(DateAndTimeUtils.getDateAtStartOrEnd(source.getEndDate(), "end", timezone));
        } catch (Exception ignored) {
        }
        return target;
    }

    /**
     * @param source shift details
     * @return dto object
     */
    public static ShiftDetailsDto entityToDetailedDto(Shifts source, String timezone) {
        ShiftDetailsDto target = new ShiftDetailsDto();
        target.setId(source.getId());
        target.setStart(DateAndTimeUtils.getDateTimeObject(source.getStart(), timezone));
        target.setEnd(DateAndTimeUtils.getDateTimeObject(source.getEnd(), timezone));
        target.setNotes(source.getNotes());
        target.setShiftType(source.getShiftType());
        target.setShiftStatus(source.getShiftStatus());
        target.setConflicts(CommonUtils.commaSeparatedStringToList(source.getConflictReason()));
        return target;
    }

    /**
     * @param source Detailed shift details
     * @return dto object
     */
    public static ShiftDetailsDto detailedEntityToDto(DetailedShift source) {
        ShiftDetailsDto target = new ShiftDetailsDto();
        target.setId(source.getId());
        target.setStart(DateAndTimeUtils.getDateTimeObject(source.getStart(), source.getLocation().getTimezone()));
        target.setEnd(DateAndTimeUtils.getDateTimeObject(source.getEnd(), source.getLocation().getTimezone()));
        target.setNotes(source.getNotes());
        target.setShiftType(source.getShiftType());
        target.setShiftStatus(source.getShiftStatus());
        target.setConflicts(CommonUtils.commaSeparatedStringToList(source.getConflictReason()));
        if (source.getUser() != null) {
            UserInfoForDropDown user = new UserInfoForDropDown();
            user.setId(source.getUser().getId());
            user.setEmail(source.getUser().getEmail());
            user.setFullName(CommonUtils.combineFirstAndLastName(source.getUser().getFirstName(), source.getUser().getLastName()));
            user.setAuthority(source.getUser().getAuthority());
            target.setUser(user);
        }
        LocationAndRoleDto locationAndRoleInfo = new LocationAndRoleDto();
        locationAndRoleInfo.setLocationId(source.getLocation().getId());
        locationAndRoleInfo.setLocationName(source.getLocation().getName());
        locationAndRoleInfo.setLocationTimezone(source.getLocation().getTimezone());
        locationAndRoleInfo.setLocationRoleId(source.getLocationRole().getId());
        locationAndRoleInfo.setLocationRoleName(source.getLocationRole().getName());
        target.setLocationAndRoleDetails(locationAndRoleInfo);
        return target;
    }

    /**
     * @param dto new shift details
     * @return Shift status based on details provided in dto
     */
    private static ShiftStatus getNewShiftStatus(NewShiftDto dto) {
        if (StringUtils.isNotEmpty(dto.getUserId()) && dto.isInstantRelease()) return ShiftStatus.SCHEDULED;
        else if (StringUtils.isNotEmpty(dto.getUserId())) return ShiftStatus.CREATED_ASSIGNED;
        else if (dto.isInstantRelease()) return ShiftStatus.RELEASED_UNASSIGNED;
        else return ShiftStatus.CREATED_UNASSIGNED;
    }

}
