package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.domain.locationandroles.Location;
import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.requests.DetailedRequests;
import ca.waaw.domain.requests.Requests;
import ca.waaw.domain.requests.RequestsHistory;
import ca.waaw.domain.shifts.Shifts;
import ca.waaw.domain.user.User;
import ca.waaw.domain.user.UserOrganization;
import ca.waaw.dto.appnotifications.NotificationInfoDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.requests.NewRequestDto;
import ca.waaw.dto.requests.UpdateRequestDto;
import ca.waaw.enumration.*;
import ca.waaw.enumration.request.RequestStatus;
import ca.waaw.enumration.request.RequestSubType;
import ca.waaw.enumration.shift.ShiftStatus;
import ca.waaw.enumration.shift.ShiftType;
import ca.waaw.enumration.user.Authority;
import ca.waaw.mapper.RequestsMapper;
import ca.waaw.repository.requests.DetailedRequestsRepository;
import ca.waaw.repository.locationandroles.LocationRepository;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.requests.RequestsHistoryRepository;
import ca.waaw.repository.requests.RequestsRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.repository.shifts.ShiftsRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.service.AppNotificationService;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.application.ShiftOverlappingException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.MessageConstants;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RequestsService {

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final LocationRepository locationRepository;

    private final RequestsRepository requestsRepository;

    private final DetailedRequestsRepository detailedRequestsRepository;

    private final RequestsHistoryRepository requestsHistoryRepository;

    private final ShiftsRepository shiftsRepository;

    private final ShiftsService shiftsService;

    private final OrganizationRepository organizationRepository;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppNotificationService appNotificationService;

    @Transactional(rollbackFor = Exception.class)
    public void addNewRequest(NewRequestDto newRequestDto) {
        CommonUtils.checkRoleAuthorization(Authority.MANAGER, Authority.EMPLOYEE);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    String timezone = locationRepository.findOneByIdAndDeleteFlag(loggedUser.getLocationId(), false)
                            .map(Location::getTimezone).orElseThrow(() -> new EntityNotFoundException("location"));
                    User assignedTo = !loggedUser.getAuthority().equals(Authority.MANAGER) ?
                            userRepository.findAllByAuthorityAndLocationIdAndDeleteFlag(Authority.MANAGER, loggedUser.getLocationId(), false)
                                    .stream().findFirst()
                                    .orElseGet(() -> userRepository.findAllByAuthorityAndOrganizationIdAndDeleteFlag(Authority.ADMIN, loggedUser.getOrganizationId(), false)
                                            .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("admin"))) :
                            userRepository.findAllByAuthorityAndOrganizationIdAndDeleteFlag(Authority.ADMIN, loggedUser.getOrganizationId(), false)
                                    .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("admin"));
                    String currentWaawId = requestsRepository.getLastUsedWaawId()
                            .orElse(appCustomIdConfig.getRequestPrefix() + "0000000000");
                    Requests request = RequestsMapper.dtoToNewEntity(newRequestDto, loggedUser, timezone);
                    request.setAssignedTo(assignedTo.getId());
                    request.setWaawId(CommonUtils.getNextCustomId(currentWaawId, appCustomIdConfig.getLength()));
                    requestsRepository.save(request);
                    RequestsHistory history = RequestsMapper.dtoToNewHistoryEntity(newRequestDto, loggedUser, request.getId());
                    requestsHistoryRepository.save(history);
                    notifyUserOrAdmin(request, loggedUser, assignedTo);
                    return request;
                })
                .map(request -> CommonUtils.logMessageAndReturnObject(request, "info", RequestsService.class,
                        "New Request saved successfully: {}", request))
                .orElseThrow(AuthenticationException::new);
    }

    public PaginationDto getAllRequests(int pageNo, int pageSize, String searchKey, String locationId, String startDate,
                                        String endDate, String status, String type) {
        CommonUtils.checkRoleAuthorization(Authority.MANAGER, Authority.ADMIN);
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
        Instant[] startEnd = StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone) : new Instant[]{null, null};
        if (loggedUser.getAuthority().equals(Authority.MANAGER)) locationId = loggedUser.getLocationId();
        Page<DetailedRequests> requestsPage = detailedRequestsRepository.searchAndFilter(loggedUser.getOrganizationId(),
                locationId, null, type, status, searchKey,
                loggedUser.getAuthority().equals(Authority.ADMIN), startEnd[0], startEnd[1], getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(requestsPage, RequestsMapper::entityToDto, timezone);
    }

    public PaginationDto getAllRequestsForUser(int pageNo, int pageSize, String userId, String startDate,
                                               String endDate, String status, String type) {
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").ascending());
        UserOrganization loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        String timezone = SecurityUtils.isCurrentUserInRole(Authority.ADMIN) ?
                loggedUser.getOrganization().getTimezone() : loggedUser.getLocation().getTimezone();
        Instant[] startEnd = StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate) ?
                DateAndTimeUtils.getStartAndEndTimeForInstant(startDate, endDate, timezone) : new Instant[]{null, null};
        if (loggedUser.getAuthority().equals(Authority.ADMIN) && StringUtils.isEmpty(userId)) {
            throw new BadRequestException("UserId is required");
        } else if (StringUtils.isEmpty(userId) || loggedUser.getAuthority().equals(Authority.EMPLOYEE)) {
            userId = loggedUser.getId();
        }
        Page<DetailedRequests> requestsPage = detailedRequestsRepository.searchAndFilter(loggedUser.getOrganizationId(),
                null, userId, type, status, null,
                true, startEnd[0], startEnd[1], getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(requestsPage, RequestsMapper::entityToDto, timezone);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRequest(UpdateRequestDto dto) {
        DetailedRequests request = detailedRequestsRepository.findOneByIdAndDeleteFlag(dto.getId(), false)
                .orElseThrow(() -> new EntityNotFoundException("request"));
        User loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .orElseThrow(AuthenticationException::new);
        if ((loggedUser.getAuthority().equals(Authority.ADMIN) &&
                !loggedUser.getOrganizationId().equals(request.getOrganizationId())) ||
                (loggedUser.getAuthority().equals(Authority.MANAGER) &&
                        !loggedUser.getLocationId().equals(request.getLocation().getId())) ||
                (((loggedUser.getAuthority().equals(Authority.EMPLOYEE)) || loggedUser.getAuthority().equals(Authority.CONTRACTOR)) &&
                        !request.getUser().getId().equals(loggedUser.getId()))) {
            throw new EntityNotFoundException("request");
        }
        switch (dto.getResponse()) {
            case REJECT:
                request.setStatus(RequestStatus.DENIED);
                break;
            case REFER_BACK:
                request.setStatus(RequestStatus.OPEN);
                break;
            case APPROVE:
                request.setStatus(RequestStatus.ACCEPTED);
                switch (request.getType()) {
                    case TIME_OFF:
                        createNewTimeOff(request, loggedUser);
                        break;
                    case OVERTIME:
                        createNewOvertime(request, loggedUser);
                        break;
                }
                break;
        }
        RequestsHistory history = RequestsMapper.dtoToNewHistoryEntity(dto, loggedUser, request.getId());
        requestsHistoryRepository.save(history);
        detailedRequestsRepository.save(request);
        switch (dto.getResponse()) {
            case REJECT:
                notifyUserOrAdmin(request, loggedUser, request.getUser(), "reject");
                break;
            case REFER_BACK:
                boolean sendToEmployee = !loggedUser.getId().equals(request.getUser().getId());
                notifyUserOrAdmin(request, sendToEmployee ? request.getAssignedTo() : loggedUser,
                        sendToEmployee ? request.getUser() : request.getAssignedTo(), "respond");
                break;
            case APPROVE:
                notifyUserOrAdmin(request, loggedUser, request.getUser(), "approve");
        }
    }

    private void createNewTimeOff(DetailedRequests request, User loggedUser) {
        if (request.getSubType().equals(RequestSubType.VACATION_LEAVE_FULL_DAY) ||
                request.getSubType().equals(RequestSubType.SICK_LEAVE_FULL_DAY)) {
            List<Shifts> newShifts = shiftsRepository.getByUserIdBetweenDates(request.getUser().getId(), request.getStart(), request.getEnd())
                    .stream().peek(shift -> {
                        shift.setNotes("Timeoff request for " + request.getUser().getFullName() + " has been approved. Shift was moved to unassigned state.");
                        shift.setShiftStatus(ShiftStatus.CREATED);
                        shift.setLastModifiedBy(loggedUser.getId());
                        shift.setUserId(null);
                    }).collect(Collectors.toList());
            shiftsRepository.saveAll(newShifts);
        } else {
            shiftsRepository.getSingleByUserIdBetweenDates(request.getUser().getId(), request.getStart(), request.getEnd())
                    .map(shift -> {
                        if (request.getStart().isAfter(shift.getStart()) && request.getEnd().isBefore(shift.getEnd())) {
                            Shifts newShift1 = new Shifts();
                            BeanUtils.copyProperties(shift, newShift1);
                            newShift1.setId(UUID.randomUUID().toString());
                            newShift1.setStart(shift.getStart());
                            newShift1.setEnd(request.getStart());
                            newShift1.setCreatedBy(loggedUser.getId());
                            String currentCustomId = shiftsRepository.getLastUsedShiftId()
                                    .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000");
                            newShift1.setWaawShiftId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
                            newShift1.setNotes("New shift created after half day timeoff request was approved.");
                            Shifts newShift2 = new Shifts();
                            BeanUtils.copyProperties(shift, newShift2);
                            newShift2.setId(UUID.randomUUID().toString());
                            newShift2.setStart(request.getEnd());
                            newShift2.setEnd(shift.getEnd());
                            newShift2.setCreatedBy(loggedUser.getId());
                            newShift2.setWaawShiftId(CommonUtils.getNextCustomId(newShift1.getWaawShiftId(), appCustomIdConfig.getLength()));
                            newShift2.setNotes("New shift created after half day timeoff request was approved.");
                            shift.setStart(request.getStart());
                            shift.setEnd(request.getEnd());
                            shiftsRepository.save(newShift1);
                            shiftsRepository.save(newShift2);
                        } else if (request.getStart().isAfter(shift.getStart())) {
                            Shifts newShift = new Shifts();
                            BeanUtils.copyProperties(shift, newShift);
                            newShift.setId(UUID.randomUUID().toString());
                            newShift.setStart(shift.getStart());
                            newShift.setEnd(request.getStart());
                            newShift.setCreatedBy(loggedUser.getId());
                            String currentCustomId = shiftsRepository.getLastUsedShiftId()
                                    .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000");
                            newShift.setWaawShiftId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
                            newShift.setNotes("New shift created after half day timeoff request was approved.");
                            shift.setStart(request.getStart());
                            shiftsRepository.save(newShift);
                        } else {
                            Shifts newShift = new Shifts();
                            BeanUtils.copyProperties(shift, newShift);
                            newShift.setId(UUID.randomUUID().toString());
                            newShift.setStart(request.getEnd());
                            newShift.setEnd(shift.getEnd());
                            newShift.setCreatedBy(loggedUser.getId());
                            String currentCustomId = shiftsRepository.getLastUsedShiftId()
                                    .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000");
                            newShift.setWaawShiftId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
                            newShift.setNotes("New shift created after half day timeoff request was approved.");
                            shift.setEnd(request.getEnd());
                            shiftsRepository.save(newShift);
                        }
                        shift.setNotes("Half day timeoff request for " + request.getUser().getFullName() + " has been approved. Half of the shift was moved to unassigned state.");
                        shift.setShiftStatus(ShiftStatus.CREATED);
                        shift.setLastModifiedBy(loggedUser.getId());
                        shift.setUserId(null);
                        return shift;
                    }).map(shiftsRepository::save);
        }
    }

    private void createNewOvertime(DetailedRequests request, User loggedUser) {
        if (shiftsRepository.getSingleByUserIdBetweenDates(request.getUser().getId(), request.getStart(), request.getEnd())
                .isPresent()) {
            throw new ShiftOverlappingException();
        }
        Organization organization = organizationRepository.findOneByIdAndDeleteFlag(loggedUser.getOrganizationId(), false)
                .orElseThrow(() -> new EntityNotFoundException("organization"));
        Shifts shift = new Shifts();
        shift.setShiftStatus(ShiftStatus.RELEASED);
        shift.setWaawBatchId(shiftsService.getNewBatchId(organization));
        shift.setBatchName("Overtime");
        shift.setStart(request.getStart());
        shift.setBatchStart(request.getStart());
        shift.setEnd(request.getEnd());
        shift.setBatchEnd(request.getEnd());
        shift.setNotes("Overtime approved by " + loggedUser.getFullName());
        shift.setShiftType(ShiftType.OVERTIME);
        shift.setBatchId(UUID.randomUUID().toString());
        shift.setCreatedBy(loggedUser.getId());
        shift.setOrganizationId(organization.getId());
        shift.setLocationId(request.getUser().getLocationId());
        shift.setLocationRoleId(request.getUser().getLocationRoleId());
        shift.setUserId(request.getUser().getId());
        String currentCustomId = shiftsRepository.getLastUsedShiftId()
                .orElse(appCustomIdConfig.getShiftPrefix() + "0000000000");
        shift.setWaawShiftId(CommonUtils.getNextCustomId(currentCustomId, appCustomIdConfig.getLength()));
        shiftsRepository.save(shift);
    }

    private void notifyUserOrAdmin(Requests request, User actionTaker, User sendTo) {
        NotificationInfoDto notificationInfo = NotificationInfoDto
                .builder()
                .receiverUuid(sendTo.getId())
                .receiverUsername(sendTo.getUsername())
                .receiverName(sendTo.getFullName())
                .receiverMail(sendTo.getEmail())
                .receiverMobile(sendTo.getMobile() == null ? null : sendTo.getCountryCode() + sendTo.getMobile())
                .language(sendTo.getLangKey() == null ? null : sendTo.getLangKey())
                .type(NotificationType.REQUEST)
                .build();
        String requestType = request.getType().toString().toLowerCase().replaceAll("_", " ");
        appNotificationService.sendApplicationNotification(MessageConstants.newRequest, notificationInfo, false,
                actionTaker.getFullName(), requestType);
    }

    private void notifyUserOrAdmin(DetailedRequests request, User actionTaker, User sendTo, String type) {
        NotificationInfoDto notificationInfo = NotificationInfoDto
                .builder()
                .receiverUuid(sendTo.getId())
                .receiverName(sendTo.getFullName())
                .receiverUsername(sendTo.getUsername())
                .receiverMail(sendTo.getEmail())
                .receiverMobile(sendTo.getMobile() == null ? null : sendTo.getCountryCode() + sendTo.getMobile())
                .language(sendTo.getLangKey() == null ? null : sendTo.getLangKey())
                .type(NotificationType.REQUEST)
                .build();
        String[] property;
        switch (type.toLowerCase()) {
            case "reject":
                property = MessageConstants.rejectRequest;
                break;
            case "approve":
                property = MessageConstants.acceptRequest;
                break;
            case "respond":
                property = MessageConstants.respondToRequest;
                break;
            default:
                return;
        }
        String requestType = request.getType().toString().toLowerCase().replaceAll("_", " ");
        appNotificationService.sendApplicationNotification(property, notificationInfo, false,
                actionTaker.getFullName(), requestType);
    }

}