package ca.waaw.web.rest.service;

import ca.waaw.config.applicationconfig.AppCustomIdConfig;
import ca.waaw.config.applicationconfig.AppInvoiceConfig;
import ca.waaw.domain.payments.PaymentHistory;
import ca.waaw.domain.user.User;
import ca.waaw.dto.payments.CreditCardDto;
import ca.waaw.dto.appnotifications.MailDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.payments.NewPaymentDto;
import ca.waaw.dto.payments.PaymentsDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.payment.PaymentStatus;
import ca.waaw.enumration.payment.TransactionType;
import ca.waaw.payment.stripe.StripeService;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.payments.PaymentHistoryRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.repository.user.UserOrganizationRepository;
import ca.waaw.security.SecurityUtils;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.service.email.javamailsender.TempMailService;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import ca.waaw.web.rest.utils.CommonUtils;
import ca.waaw.web.rest.utils.DateAndTimeUtils;
import ca.waaw.web.rest.utils.MessageConstants;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentsService {

    private final Logger log = LogManager.getLogger(PaymentsService.class);

    private final UserRepository userRepository;

    private final UserOrganizationRepository userOrganizationRepository;

    private final StripeService stripeService;

    private final PaymentHistoryRepository paymentHistoryRepository;

    private final OrganizationRepository organizationRepository;

    private final TempMailService tempMailService;

    private final AppCustomIdConfig appCustomIdConfig;

    private final AppInvoiceConfig appInvoiceConfig;

    private final TokenProvider tokenProvider;

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

    public void updateDefaultCard(String cardId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> {
                    try {
                        stripeService.updateDefaultCard(loggedUser.getStripeId(), cardId);
                        return loggedUser;
                    } catch (StripeException e) {
                        log.error("Exception while updating default card for stripe user: {}",
                                loggedUser, e);
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

    public void createNewInvoice(NewPaymentDto newPaymentDto) {
        String currentInvoiceId = paymentHistoryRepository.getLastUsedInvoiceId()
                .orElse(appCustomIdConfig.getInvoicePrefix() + "0000000000");
        PaymentHistory invoice = new PaymentHistory();
        invoice.setInvoiceDate(Instant.now());
        invoice.setDueDate(newPaymentDto.getType().equals(TransactionType.PLATFORM_FEE) ? newPaymentDto.getPaymentDate() :
                newPaymentDto.getPaymentDate().plus(appInvoiceConfig.getAllowDaysBeforeDueDate(), ChronoUnit.DAYS));
        invoice.setOrganizationId(newPaymentDto.getOrganizationId());
        invoice.setQuantity(newPaymentDto.getQuantity());
        invoice.setUnitPrice(newPaymentDto.getUnitPrice());
        invoice.setTotalAmount(newPaymentDto.getTotalAmount());
        invoice.setCurrency(newPaymentDto.getCurrency());
        invoice.setTransactionType(newPaymentDto.getType());
        invoice.setInvoiceId(CommonUtils.getNextCustomId(currentInvoiceId, appCustomIdConfig.getLength()));
        paymentHistoryRepository.save(invoice);
    }

    // TODO Add filters
    public PaginationDto getFullPaymentHistory(int pageNo, int pageSize, String status, String startDate, String endDate) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("invoiceDate").descending());
        AtomicReference<String> timezone = new AtomicReference<>();
        Page<PaymentHistory> paymentsPage = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userOrganizationRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> {
                    timezone.set(user.getOrganization().getTimezone());
                    return paymentHistoryRepository.findAllByOrganizationId(user.getOrganizationId(), getSortedByCreatedDate);
                }).orElse(Page.empty());
        return CommonUtils.getPaginationResponse(paymentsPage, (payment) -> {
            PaymentsDto response = new PaymentsDto();
            BeanUtils.copyProperties(payment, response);
            response.setInvoiceDate(DateAndTimeUtils.getFullMonthDate(payment.getInvoiceDate(), timezone.get()));
            response.setDateRange(
                    payment.getInvoiceStart() == null ? "-" :
                            DateAndTimeUtils.getFullMonthDate(payment.getInvoiceStart(), timezone.get()) + " - " +
                                    DateAndTimeUtils.getFullMonthDate(payment.getInvoiceEnd(), timezone.get())
            );
            response.setDueDate(DateAndTimeUtils.getFullMonthDateWithTime(payment.getDueDate(), timezone.get()));
            response.setPaymentDate(payment.getPaymentDate() == null ? "-" : DateAndTimeUtils.getFullMonthDate(payment.getPaymentDate(), timezone.get()));
            return response;
        });
    }

    public PaymentsDto getPaymentHistoryById(String paymentId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(user -> paymentHistoryRepository.findOneByIdAndOrganizationId(paymentId, user.getOrganizationId())
                        .map(payment -> {
                            PaymentsDto response = new PaymentsDto();
                            BeanUtils.copyProperties(payment, response);
                            return response;
                        })
                        .orElseThrow(() -> new EntityNotFoundException("payment"))
                ).orElseThrow(AuthenticationException::new);
    }

    public PaymentsDto getPendingPayment() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> paymentHistoryRepository.findOneByOrganizationIdAndPaymentStatus(loggedUser.getOrganizationId(),
                                PaymentStatus.UNPAID)
                        .map(payment -> {
                            PaymentsDto response = new PaymentsDto();
                            BeanUtils.copyProperties(payment, response);
                            return response;
                        })
                        .orElseThrow(() -> new EntityNotFoundException("invoice"))
                )
                .orElseThrow(AuthenticationException::new);
    }

    public Map<String, String> createNewPaymentIntent(String paymentId) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .map(loggedUser -> paymentHistoryRepository.findOneByIdAndOrganizationId(paymentId, loggedUser.getOrganizationId())
                        .map(payment -> {
                            try {
                                if (payment.getPaymentStatus().equals(PaymentStatus.PAID)) {
                                    throw new BadRequestException("");// TODO throw already paid exception(conflict)
                                }
                                if (StringUtils.isNotEmpty(payment.getStripeId())) {
                                    stripeService.cancelPaymentIntent(payment.getStripeId());
                                }
                                Map<String, String> response = stripeService.createNewPaymentIntent(loggedUser.getStripeId(),
                                        payment.getTotalAmount(), payment.getCurrency().toString(), payment.getInvoiceId(),
                                        payment.getTransactionId(), loggedUser.getEmail());
                                payment.setStripeId(response.get("intentId"));
                                paymentHistoryRepository.save(payment);
                                response.remove("intentId");
                                return response;
                            } catch (StripeException e) {
                                log.error("Exception while creating payment intent with paymentId({}) for stripe user: {}", paymentId, loggedUser);
                                return null;
                            }
                        })
                        .orElseThrow(() -> new EntityNotFoundException("invoice"))
                ).orElseThrow(AuthenticationException::new);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto confirmPayment(String paymentId, boolean success) {
        CommonUtils.checkRoleAuthorization(Authority.ADMIN);
        User loggedUser = SecurityUtils.getCurrentUserLogin()
                .flatMap(username -> userRepository.findOneByUsernameAndDeleteFlag(username, false))
                .flatMap(user -> paymentHistoryRepository.findOneByIdAndOrganizationId(paymentId, user.getOrganizationId())
                        .map(payment -> {
                            String currentTransactionId = paymentHistoryRepository.getLastUsedTransactionId()
                                    .orElse(appCustomIdConfig.getTransactionPrefix() + "0000000000");
                            payment.setTransactionId(CommonUtils.getNextCustomId(currentTransactionId, appCustomIdConfig.getLength()));
                            if (success) {
                                payment.setPaymentDate(Instant.now());
                                payment.setPaymentStatus(PaymentStatus.PAID);
                            } else {
                                payment.setPaymentStatus(PaymentStatus.FAILED);
                            }
                            return payment;
                        }).map(paymentHistoryRepository::save)
                        .map(payment -> {
                            if (!success) {
                                PaymentHistory newPayment = new PaymentHistory();
                                BeanUtils.copyProperties(payment, newPayment);
                                newPayment.setId(UUID.randomUUID().toString());
                                newPayment.setPaymentStatus(PaymentStatus.UNPAID);
                                newPayment.setStripeId(null);
                                paymentHistoryRepository.save(newPayment);
                            }
                            return payment;
                        })
                        .flatMap(payment -> {
                            if (success) {
                                return organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false)
                                        .map(organization -> {
                                            if (payment.getTransactionType().equals(TransactionType.PLATFORM_FEE))
                                                organization.setPlatformFeePaid(true);
                                            if (organization.isPaymentPending()) {
                                                organization.setPaymentPending(false);
                                                CompletableFuture.runAsync(() -> {
                                                    List<User> usersToUpdate = userRepository.findAllByAccountStatusAndOrganizationIdAndDeleteFlag(AccountStatus.PAYMENT_PENDING,
                                                                    organization.getId(), false)
                                                            .stream()
                                                            .peek(user1 -> {
                                                                user1.setAccountStatus(AccountStatus.PAID_AND_ACTIVE);
                                                                user1.setLastModifiedBy(user.getId());
                                                            })
                                                            .collect(Collectors.toList());
                                                    userRepository.saveAll(usersToUpdate);
                                                });
                                            }
                                            // TODO change 1 days to month
                                            // TODO if due date on payment has passed add time to now to create next payment date minus allowed buffer
                                            organization.setNextPaymentOn((payment.getDueDate().isBefore(Instant.now()) ?
                                                    Instant.now() : organization.getNextPaymentOn()).plus(1, ChronoUnit.DAYS));
                                            organizationRepository.save(organization);
                                            user.setAccountStatus(AccountStatus.PAID_AND_ACTIVE);
                                            return userRepository.save(user);
                                        });
                            }
                            return Optional.of(user);
                        })
                )
                .orElseThrow(UnauthorizedException::new);
        final String jwt = tokenProvider.updateUsernameOrStatusInToken(loggedUser.getUsername(), loggedUser.getAccountStatus());
        return new LoginResponseDto(jwt);
    }

    public void notifyUserAboutAccountSuspension(User user, PaymentHistory payment, String timezone) {
        String[] messageContent = payment.getTransactionType().equals(TransactionType.PLATFORM_FEE) ?
                MessageConstants.accountSuspendedPlatformInvoice : MessageConstants.accountSuspendedMonthlyInvoice;
        MailDto mailInfo = MailDto.builder()
                .email(user.getEmail())
                .name(user.getFullName())
                .langKey(user.getLangKey())
                .build();
        String[] messageArgs;
        if (payment.getTransactionType().equals(TransactionType.PLATFORM_FEE)) {
            messageArgs = new String[]{
                    payment.getInvoiceId(),
                    String.format("%s %s", payment.getTotalAmount(), payment.getCurrency()),
                    DateAndTimeUtils.getDateTimeObject(payment.getDueDate(), timezone).getDate()
            };
        } else {
            messageArgs = new String[]{
                    payment.getInvoiceId(),
                    String.valueOf(payment.getQuantity()),
                    DateAndTimeUtils.getDateTimeObject(payment.getInvoiceStart(), timezone).getDate(),
                    DateAndTimeUtils.getDateTimeObject(payment.getInvoiceEnd(), timezone).getDate(),
                    DateAndTimeUtils.getDateTimeObject(payment.getDueDate(), timezone).getDate(),
                    String.format("%s %s", payment.getTotalAmount(), payment.getCurrency())
            };
        }
        tempMailService.sendEmailFromTemplate(mailInfo, messageContent, null, messageArgs);
    }

}