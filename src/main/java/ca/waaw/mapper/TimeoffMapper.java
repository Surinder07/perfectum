//package ca.waaw.mapper;
//
//import ca.waaw.domain.TimeOffs;
//import ca.waaw.domain.joined.DetailedTimeOff;
//import ca.waaw.dto.locationandroledtos.LocationAndRoleDto;
//import ca.waaw.dto.timeoff.NewTimeOffDto;
//import ca.waaw.dto.timeoff.TimeOffInfoDto;
//import ca.waaw.dto.userdtos.UserInfoForDropDown;
//import ca.waaw.enumration.TimeOffType;
//import ca.waaw.web.rest.utils.CommonUtils;
//import ca.waaw.web.rest.utils.DateAndTimeUtils;
//
//public class TimeoffMapper {
//
//    /**
//     * @param source   source dto
//     * @param timezone location timezone of employee
//     * @return Timeoff entity
//     */
//    public static TimeOffs dtoToEntity(NewTimeOffDto source, String timezone) {
//        TimeOffs target = new TimeOffs();
//        target.setStartDate(DateAndTimeUtils.getDateInstant(source.getStartDate().getDate(),
//                source.getStartDate().getTime(), timezone));
//        target.setStartDate(DateAndTimeUtils.getDateInstant(source.getEndDate().getDate(),
//                source.getEndDate().getTime(), timezone));
//        target.setNotes(source.getNote());
//        target.setUserId(source.getUserId());
//        target.setType(TimeOffType.valueOf(source.getType()));
//        return target;
//    }
//
//    /**
//     * @param source   detailed entity
//     * @param timezone timezone for logged-in user
//     * @return time details dto
//     */
//    public static TimeOffInfoDto entityToDto(DetailedTimeOff source, String timezone) {
//        TimeOffInfoDto target = new TimeOffInfoDto();
//        target.setId(source.getId());
//        target.setStartDate(DateAndTimeUtils.getDateTimeObject(source.getStartDate(), timezone));
//        target.setEndDate(DateAndTimeUtils.getDateTimeObject(source.getEndDate(), timezone));
//        target.setNote(source.getNotes());
//        target.setStatus(source.getStatus().toString());
//        UserInfoForDropDown user = new UserInfoForDropDown();
//        user.setId(source.getUserDetails().getId());
//        user.setEmail(source.getUserDetails().getEmail());
//        user.setFullName(CommonUtils.combineFirstAndLastName(source.getUserDetails().getFirstName(), source.getUserDetails().getLastName()));
////        user.setAuthority(source.getUserDetails().getAuthority());
//        target.setUser(user);
////        LocationAndRoleDto locationAndRoleInfo = LocationRoleMapper.locationEntityToDetailDto(source.getUserDetails().getLocation(),
////                source.getUserDetails().getLocationRole());
////        target.setLocationAndRole(locationAndRoleInfo);
//        return target;
//    }
//
//}
