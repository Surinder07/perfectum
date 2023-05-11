package ca.waaw.mapper;

import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.requests.RequestsHistory;
import ca.waaw.domain.user.User;
import ca.waaw.domain.requests.DetailedRequests;
import ca.waaw.dto.DateTimeDto;
import ca.waaw.dto.requests.DetailedRequestsDto;
import ca.waaw.dto.requests.NewRequestDto;
import ca.waaw.dto.requests.RequestHistoryDto;
import ca.waaw.dto.requests.UpdateRequestDto;
import ca.waaw.enumration.request.RequestResponse;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.request.RequestType;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Collectors;

public class RequestsMapper {

    public static Requests dtoToNewEntity(NewRequestDto source, User loggedUser, String timezone) {
        Requests target = new Requests();
        target.setUserId(loggedUser.getId());
        target.setOrganizationId(loggedUser.getOrganizationId());
        target.setLocationId(loggedUser.getLocationId());
        target.setLocationRoleId(loggedUser.getLocationRoleId());
        target.setDescription(source.getDescription());
        target.setType(RequestType.valueOf(source.getType()));
        target.setSubType(StringUtils.isNotEmpty(source.getSubType()) ?
                RequestSubType.valueOf(source.getSubType()) : null);
        Instant startDate = getDateByNullDateOrTime(source.getStart(), timezone, "start");
        target.setStart(startDate);
        if (StringUtils.isNotEmpty(source.getEndDate()) || source.getDuration() != 0) {
            Instant endDate = source.getDuration() == 0 ? getDateByNullDateOrTime(DateTimeDto.builder().date(source.getEndDate()).build(),
                    timezone, "end") : startDate.plus(source.getDuration(), ChronoUnit.HOURS);
            target.setEnd(endDate);
        }
        return target;
    }

    public static RequestsHistory dtoToNewHistoryEntity(NewRequestDto source, User loggedUser,
                                                        String requestId) {
        RequestsHistory target = new RequestsHistory();
        target.setDescription(source.getDescription());
        target.setRequestId(requestId);
        target.setCommentType(RequestStatus.NEW);
        target.setCommenterId(loggedUser.getId());
        target.setCommenterName(loggedUser.getFullName());
        return target;
    }

    public static RequestsHistory dtoToNewHistoryEntity(UpdateRequestDto source, User loggedUser,
                                                        String requestId) {
        RequestsHistory target = new RequestsHistory();
        target.setDescription(source.getComment());
        target.setRequestId(requestId);
        target.setCommentType(getRequestStatus(source.getResponse()));
        target.setCommenterId(loggedUser.getId());
        target.setCommenterName(loggedUser.getFullName());
        return target;
    }

    public static DetailedRequestsDto entityToDto(DetailedRequests source, String timezone) {
        DetailedRequestsDto target = new DetailedRequestsDto();
        target.setId(source.getId());
        target.setStatus(source.getStatus().toString());
        target.setAssignedTo(source.getAssignedTo().getFullName());
        target.setCreatedDate(DateAndTimeUtils.getFullMonthDate(source.getCreatedDate(), timezone));
        target.setType(source.getType().toString());
        target.setSubType(source.getSubType() == null ? null : source.getSubType().toString());
        target.setLocation(source.getLocation().getName());
        target.setWaawId(source.getWaawId());
        target.setRaisedBy(source.getUser().getFullName());
        target.setHistory(
                source.getHistory().stream()
                        .sorted(Comparator.comparing(RequestsHistory::getCreatedDate))
                        .map(his -> historyEntityToDto(his, timezone, source.getType().toString(),
                                source.getStart(), source.getEnd(), source.getSubType()))
                        .collect(Collectors.toList())
        );
        return target;
    }

    private static RequestHistoryDto historyEntityToDto(RequestsHistory history, String timezone, String requestType,
                                                        Instant start, Instant end, RequestSubType subType) {
        RequestHistoryDto historyDto = new RequestHistoryDto();
        String action = "";
        String timeframe = getTimeFrameForRequest(subType, requestType, start, end, timezone);
        switch (history.getCommentType()) {
            case NEW:
                action = " raised a request for " + (requestType.charAt(0) + requestType.substring(1).toLowerCase())
                        .replaceAll("_", " ") + timeframe;
                break;
            case OPEN:
                action = " responded to the request";
                break;
            case ACCEPTED:
                action = " accepted the request";
                break;
            case DENIED:
                action = " rejected the request";
                break;
        }
        String title = history.getCommenterName() + action;
        historyDto.setId(history.getId());
        historyDto.setTitle(title);
        historyDto.setStatus(history.getCommentType().toString());
        historyDto.setDescription(history.getDescription());
        historyDto.setDate(DateAndTimeUtils.getDateWithFullMonth(history.getCreatedDate(), timezone));
        return historyDto;
    }

    private static String getTimeFrameForRequest(RequestSubType subType, String type, Instant start, Instant end, String timezone) {
        if (type.equalsIgnoreCase(RequestType.INFORMATION_UPDATE.toString())) return "";
        else if (type.equalsIgnoreCase(RequestType.TIME_OFF.toString()) &&
                (subType.equals(RequestSubType.SICK_LEAVE_FULL_DAY) ||
                        subType.equals(RequestSubType.VACATION_LEAVE_FULL_DAY))) {
            return DateAndTimeUtils.isSameDay(start, end, timezone) ? " on " + DateAndTimeUtils.getDateWithFullMonth(start, timezone)
                    : " from " + DateAndTimeUtils.getDateWithFullMonth(start, timezone) + " to " +
                    DateAndTimeUtils.getDateWithFullMonth(end, timezone);
        } else if (DateAndTimeUtils.isSameDay(start, end, timezone)) {
            return " on " + DateAndTimeUtils.getDateWithFullMonth(start, timezone) +
                    " from " + DateAndTimeUtils.getDateTimeObject(start, timezone).getTime() + " to " +
                    DateAndTimeUtils.getDateTimeObject(end, timezone).getTime();
        } else {
            return " from " + DateAndTimeUtils.getDateWithFullMonth(start, timezone) +
                    " " + DateAndTimeUtils.getDateTimeObject(start, timezone).getTime() + " to " +
                    DateAndTimeUtils.getDateWithFullMonth(end, timezone) + " " +
                    DateAndTimeUtils.getDateTimeObject(end, timezone).getTime();
        }
    }

    private static Instant getDateByNullDateOrTime(DateTimeDto dateTimeDto, String timezone, String type) {
        if (dateTimeDto == null) return null;
        else if (StringUtils.isEmpty(dateTimeDto.getTime())) {
            Instant[] dates = DateAndTimeUtils.getStartAndEndTimeForInstant(dateTimeDto.getDate(), timezone);
            return type.equalsIgnoreCase("start") ? dates[0] : dates[1];
        } else {
            return DateAndTimeUtils.getDateInstant(dateTimeDto.getDate(), dateTimeDto.getTime(), timezone);
        }
    }

    private static RequestStatus getRequestStatus(RequestResponse response) {
        switch (response) {
            case APPROVE:
                return RequestStatus.ACCEPTED;
            case REFER_BACK:
                return RequestStatus.OPEN;
            case REJECT:
                return RequestStatus.DENIED;
        }
        return null;
    }

}
