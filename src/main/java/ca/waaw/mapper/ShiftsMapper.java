package ca.waaw.mapper;

import ca.waaw.domain.ShiftBatchUserMapping;
import ca.waaw.domain.Shifts;
import ca.waaw.domain.ShiftsBatch;
import ca.waaw.domain.User;
import ca.waaw.domain.joined.DetailedShift;
import ca.waaw.domain.joined.UserOrganization;
import ca.waaw.dto.OvertimeDto;
import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
import ca.waaw.dto.shifts.BatchDetailsDto;
import ca.waaw.dto.shifts.NewShiftBatchDto;
import ca.waaw.dto.shifts.NewShiftDto;
import ca.waaw.dto.shifts.ShiftDetailsDto;
import ca.waaw.dto.userdtos.UserInfoForDropDown;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.ShiftStatus;
import ca.waaw.enumration.ShiftType;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
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
        if (StringUtils.isNotEmpty(source.getUserId())) target.setUserId(source.getUserId());
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
        LocationAndRoleDto locationAndRoleInfo = LocationRoleMapper.locationEntityToDetailDto(source.getLocation(),
                source.getLocationRole());
        target.setLocationAndRoleDetails(locationAndRoleInfo);
        return target;
    }

    public static BatchDetailsDto batchEntityToDto(ShiftsBatch source, UserOrganization admin, List<User> users) {
        BatchDetailsDto target = new BatchDetailsDto();
        target.setId(source.getId());
        target.setBatchName(source.getName());
        String timezone = StringUtils.isNotEmpty(admin.getLocation().getTimezone()) ? admin.getLocation().getTimezone() :
                admin.getOrganization().getTimezone();
        target.setStartDate(DateAndTimeUtils.getDateTimeObject(source.getStartDate(), timezone).getDate());
        target.setEndDate(DateAndTimeUtils.getDateTimeObject(source.getEndDate(), timezone).getDate());
        LocationAndRoleDto locationAndRoleDto = new LocationAndRoleDto();
        if (users != null) {
            target.setUsers(users.stream().map(user -> {
                UserInfoForDropDown targetUser = new UserInfoForDropDown();
                targetUser.setId(user.getId());
                targetUser.setEmail(user.getEmail());
                targetUser.setFullName(CommonUtils.combineFirstAndLastName(user.getFirstName(), user.getLastName()));
                targetUser.setAuthority(user.getAuthority());
                return targetUser;
            }).collect(Collectors.toList()));
        } else if (source.getLocationRole() != null) {
            locationAndRoleDto.setLocationRoleName(source.getLocationRole().getName());
            locationAndRoleDto.setLocationRoleId(source.getLocationRole().getId());
        } else if (source.getLocation() != null) {
            locationAndRoleDto.setLocationName(source.getLocation().getName());
            locationAndRoleDto.setLocationId(source.getLocation().getId());
            locationAndRoleDto.setLocationTimezone(source.getLocation().getTimezone());
        }
        target.setLocationAndRoleDetails(locationAndRoleDto);
        target.setReleased(source.isReleased());
        UserInfoForDropDown targetCreatedBy = new UserInfoForDropDown();
        targetCreatedBy.setId(admin.getId());
        targetCreatedBy.setEmail(admin.getEmail());
        targetCreatedBy.setFullName(CommonUtils.combineFirstAndLastName(admin.getFirstName(), admin.getLastName()));
        targetCreatedBy.setAuthority(admin.getAuthority());
        target.setBatchCreatedBy(targetCreatedBy);
        return target;
    }

    /**
     * @param source     overtime info
     * @param userSource logged-in user info
     * @return Entity to be saved in database
     */
    public static Shifts overtimeDtoTOEntity(OvertimeDto source, UserOrganization userSource) {
        Authority userRole = userSource.getAuthority();
        Shifts target = new Shifts();
        String timezone;
        if (userRole.equals(Authority.ADMIN)) {
            target.setUserId(source.getUserId());
            timezone = userSource.getOrganization().getTimezone();
        } else if (userRole.equals(Authority.MANAGER)) {
            target.setUserId(source.getUserId());
            timezone = userSource.getLocation().getTimezone();
            target.setLocationId(userSource.getLocationId());
        } else {
            target.setUserId(userSource.getId());
            timezone = userSource.getLocation().getTimezone();
            target.setLocationId(userSource.getLocationId());
            target.setLocationRoleId(userSource.getLocationRoleId());
        }
        target.setStart(DateAndTimeUtils.getDateInstant(source.getStart().getDate(), source.getStart().getTime(), timezone));
        target.setEnd(DateAndTimeUtils.getDateInstant(source.getEnd().getDate(), source.getEnd().getTime(), timezone));
        target.setNotes(source.getNote());
        target.setShiftType(ShiftType.OVERTIME);
        target.setOrganizationId(userSource.getOrganizationId());
        target.setShiftStatus(userRole.equals(Authority.ADMIN) || userRole.equals(Authority.MANAGER) ?
                ShiftStatus.SCHEDULED : ShiftStatus.OVERTIME_REQUESTED);
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
