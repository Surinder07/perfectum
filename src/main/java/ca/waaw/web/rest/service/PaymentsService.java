package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppInvoiceConfig;
import ca.waaw.domain.Invoices;
import ca.waaw.domain.User;
import ca.waaw.dto.CreditCardDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.invoices.InvoiceDto;
import ca.waaw.dto.invoices.NewInvoiceDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.enumration.AccountStatus;
import ca.waaw.enumration.Authority;
import ca.waaw.enumration.InvoiceStatus;
import ca.waaw.enumration.TransactionType;
import ca.waaw.payment.stripe.StripeService;
import ca.waaw.repository.InvoicesRepository;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentsService {

    private final Logger log = LogManager.getLogger(PaymentsService.class);

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final StripeService stripeService;

    private final InvoicesRepository invoicesRepository;

    private final OrganizationRepository organizationRepository;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppInvoiceConfig appInvoiceConfig;

    private final TokenProvider tokenProvider;

    public Map<String, String> createNewSetupIntent() {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    try {
                        Map<String, String> response = new HashMap<>();
                        response.put("clientSecret", stripeService.createNewSetupIntent(loggedUser.getStripeId()));
                        return response;
                    } catch (StripeException e) {
                        log.error("Exception while creating setup intent for stripe user: {}", loggedUser);
                        return null;
                    }
                }).orElseThrow(AuthenticationException::new);
    }

    public void addNewCard(String tokenId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    try {
                        stripeService.createNewCard(tokenId, loggedUser.getStripeId());
                        return loggedUser;
                    } catch (StripeException e) {
                        log.error("Exception while adding new card for stripe user: {}, tokenId: {}",
                                loggedUser, tokenId);
                        return null;
                    }
                }).orElseThrow(AuthenticationException::new);
    }

    public void deleteCard(String cardId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    try {
                        stripeService.deleteCard(cardId, loggedUser.getStripeId());
                        return loggedUser;
                    } catch (StripeException e) {
                        log.error("Exception while deleting card with id {} for stripe user: {}",
                                cardId, loggedUser);
                        return null;
                    }
                }).orElseThrow(() -> new EntityNotFoundException("card"));
    }

    public List<CreditCardDto> getAllCards() {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    try {
                        return stripeService.getAllCards(loggedUser.getStripeId());
                    } catch (StripeException e) {
                        log.error("Exception while fetching all cards for stripe user: {}",
                                loggedUser, e);
                        return null;
                    }
                }).orElseThrow(AuthenticationException::new);
    }

    public PaginationDto getAllInvoices(int pageNo, int pageSize, String status, String startDate, String endDate) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("invoiceDate").descending());
        AtomicReference<String> timezone = new AtomicReference<>();
        Page<Invoices> invoicePage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    timezone.set(user.getOrganization().getTimezone());
                    return invoicesRepository.findAllByOrganizationId(user.getOrganizationId(), getSortedByCreatedDate);
                }).orElse(Page.empty());
        return CommonUtils.getPaginationResponse(invoicePage, (invoice) -> {
            InvoiceDto response = new InvoiceDto();
            BeanUtils.copyProperties(invoice, response);
            response.setInvoiceDate(DateAndTimeUtils.getDateTimeObject(invoice.getInvoiceDate(), timezone.get()).getDate());
            response.setDateRange(
                    invoice.getInvoiceStart() == null ? "-" :
                    DateAndTimeUtils.getDateTimeObject(invoice.getInvoiceStart(), timezone.get()).getDate() + " - " +
                            DateAndTimeUtils.getDateTimeObject(invoice.getInvoiceEnd(), timezone.get()).getDate()
            );
            response.setDueDate(DateAndTimeUtils.getDateTimeObject(invoice.getDueDate(), timezone.get()).getDate());
            response.setPaymentDate(invoice.getPaymentDate() == null ? "-" : DateAndTimeUtils.getDateTimeObject(invoice.getPaymentDate(), timezone.get()).getDate());
            return response;
        });
    }

    public InvoiceDto getById(String invoiceId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> invoicesRepository.findOneByIdAndOrganizationId(invoiceId, user.getOrganizationId())
                        .map(invoice -> {
                            InvoiceDto response = new InvoiceDto();
                            BeanUtils.copyProperties(invoice, response);
                            return response;
                        })
                        .orElseThrow(() -> new EntityNotFoundException("invoice"))
                ).orElseThrow(AuthenticationException::new);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto confirmPayment(String invoiceId, String stripeId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        User loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> invoicesRepository.findOneByIdAndOrganizationId(invoiceId, user.getOrganizationId())
                        .map(invoice -> {
                            invoice.setPaymentDate(Instant.now());
                            invoice.setStripeId(stripeId);
                            invoice.setInvoiceStatus(InvoiceStatus.PAID);
                            return invoice;
                        }).map(invoicesRepository::save)
                        .flatMap(invoice -> organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false)
                                .map(organization -> {
                                    if (invoice.getTransactionType().equals(TransactionType.PLATFORM_FEE))
                                        organization.setPlatformFeePaid(true);
                                    if (organization.isPaymentPending()) {
                                        organization.setPaymentPending(false);
                                        List<User> usersToUpdate = userRepository.findAllByAccountStatusAndOrganizationIdAndDeleteFlag(AccountStatus.PAYMENT_PENDING,
                                                organization.getId(), false)
                                                .stream()
                                                .peek(user1 -> {
                                                    user1.setAccountStatus(AccountStatus.PAID_AND_ACTIVE);
                                                    user1.setLastModifiedBy(user.getId());
                                                })
                                                .collect(Collectors.toList());
                                        userRepository.saveAll(usersToUpdate);
                                    }
                                    organization.setNextPaymentOn(organization.getNextPaymentOn().plus(3, ChronoUnit.DAYS));
                                    organizationRepository.save(organization);
                                    //TODO Update all users to active
                                    return user;
                                })
                        )
                )
                .orElseThrow(UnauthorizedException::new);
        final String jwt = tokenProvider.updateUsernameOrStatusInToken(loggedUser.getUsername(), loggedUser.getAccountStatus());
        return new LoginResponseDto(jwt);
    }

    public InvoiceDto getPendingInvoice() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> invoicesRepository.findOneByOrganizationIdAndInvoiceStatus(loggedUser.getOrganizationId(),
                                InvoiceStatus.UNPAID)
                        .map(invoice -> {
                            InvoiceDto response = new InvoiceDto();
                            BeanUtils.copyProperties(invoice, response);
                            return response;
                        })
                        .orElseThrow(() -> new EntityNotFoundException("invoice"))
                )
                .orElseThrow(AuthenticationException::new);
    }

    public Map<String, String> createNewPaymentIntent(String invoiceId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> invoicesRepository.findOneByIdAndOrganizationId(invoiceId, loggedUser.getOrganizationId())
                        .map(invoice -> {
                            try {
                                if (invoice.getInvoiceStatus().equals(InvoiceStatus.PAID)) {
                                    throw new BadRequestException("");// TODO throw already paid exception(conflict)
                                }
                                Map<String, String> response = new HashMap<>();
                                response.put("clientSecret", stripeService.createNewPaymentIntent(loggedUser.getStripeId(),
                                        invoice.getTotalAmount(), invoice.getCurrency().toString(), invoice.getId()));
                                return response;
                            } catch (StripeException e) {
                                log.error("Exception while creating payment intent with invoiceId({}) for stripe user: {}", invoiceId, loggedUser);
                                return null;
                            }
                        })
                        .orElseThrow(() -> new EntityNotFoundException("invoice"))
                ).orElseThrow(AuthenticationException::new);
    }

    public void createNewInvoice(NewInvoiceDto newInvoiceDto) {
        String currentOrgCustomId = invoicesRepository.getLastUsedCustomId()
                .orElse(appCustomIdConfig.getInvoicePrefix() + "0000000000");
        Invoices invoice = new Invoices();
        invoice.setInvoiceDate(Instant.now());
        invoice.setDueDate(newInvoiceDto.getType().equals(TransactionType.PLATFORM_FEE) ? newInvoiceDto.getPaymentDate() :
                newInvoiceDto.getPaymentDate().plus(appInvoiceConfig.getAllowDaysBeforeDueDate(), ChronoUnit.DAYS));
        invoice.setOrganizationId(newInvoiceDto.getOrganizationId());
        invoice.setQuantity(newInvoiceDto.getQuantity());
        invoice.setUnitPrice(newInvoiceDto.getUnitPrice());
        invoice.setTotalAmount(newInvoiceDto.getTotalAmount());
        invoice.setCurrency(newInvoiceDto.getCurrency());
        invoice.setTransactionType(newInvoiceDto.getType());
        invoice.setWaawId(CommonUtils.getNextCustomId(currentOrgCustomId, appCustomIdConfig.getLength()));
        invoicesRepository.save(invoice);
    }

    public static void main(String[] args) {
//        System.out.println(LocalDate.now(ZoneId.of( "Canada/Central" )));
//        System.out.println(LocalDate.now(ZoneId.of( "Canada/Central" )).plusMonths(1));
        Instant instant = Instant.now();

        ZoneId zoneId = ZoneId.of( "Canada/Central" );
        ZonedDateTime zdt = ZonedDateTime.ofInstant( instant , zoneId );
        ZonedDateTime zdtMonthLater = zdt.plusMonths( 1 );
        System.out.println(instant);
        System.out.println(zdtMonthLater.toInstant());
    }

}