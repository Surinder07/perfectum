package ca.waaw.web.rest;

import ca.waaw.domain.organization.Organization;
import ca.waaw.domain.user.User;
import ca.waaw.dto.userdtos.LoginDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.payment.PaymentStatus;
import ca.waaw.repository.organization.OrganizationRepository;
import ca.waaw.repository.payments.PaymentHistoryRepository;
import ca.waaw.repository.user.UserRepository;
import ca.waaw.security.jwt.JWTFilter;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.web.rest.errors.ErrorVM;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.service.PaymentsService;
import ca.waaw.web.rest.service.UserService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "${api.swagger.groups.auth}")
public class AuthController {

    private final Logger log = LogManager.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final UserService userService;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    private final PaymentHistoryRepository paymentHistoryRepository;

    private final PaymentsService paymentsService;

    @SwaggerBadRequest
    @Operation(description = "${api.description.authentication}")
    @PostMapping("${api.endpoints.authentication}")
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class))})
    @ApiResponse(responseCode = "401", description = "${api.swagger.error-description.authentication}", content = @Content)
    @ApiResponse(responseCode = "402", description = "${api.swagger.error-description.trial-over}",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorVM.class))})
    public ResponseEntity<LoginResponseDto> authenticate(@Valid @RequestBody LoginDto loginDto) {
        try {
            Authentication authentication;
            try {
                authentication = authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(loginDto.getLogin(), loginDto.getPassword()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BadCredentialsException e) {
                throw new AuthenticationException();
            } catch (Exception e) {
                log.error("Exception while logging in", e);
                throw e;
            }
            Optional<User> userEntity = userRepository.getByUsernameOrEmail(loginDto.getLogin());
            AccountStatus accountStatus = userEntity
                    .map(user -> {
                        if (!user.getAccountStatus().equals(AccountStatus.PROFILE_PENDING)) {
                            Organization organization = organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false)
                                    .orElseThrow(() -> new EntityNotFoundException("organization"));
                            if (StringUtils.isEmpty(user.getStripeId()) && user.getAuthority().equals(Authority.ADMIN)) {
                                userService.addCustomerOnStripe(user, organization.getName());
                                user.setAccountStatus(AccountStatus.PAYMENT_INFO_PENDING);
                                userRepository.save(user);
                            }
                            if ((!organization.isPlatformFeePaid() && organization.getTrialEndDate().isBefore(Instant.now())) ||
                                    organization.isPaymentPending()) {
                                user.setAccountStatus(AccountStatus.PAYMENT_PENDING);
                                userRepository.save(user);
                                log.error("Payment pending for organization {}({})", organization.getName(), organization.getId());
                            } else {
                                checkForPendingPayment(organization, user);
                            }
                        }
                        return user;
                    })
                    .map(User::getAccountStatus).orElse(null);
            final String token = tokenProvider.createToken(authentication, loginDto.isRememberMe(), accountStatus);
            assert accountStatus != null;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);

            // Update last login to current time
            userEntity.map(user -> {
                user.setLastLogin(Instant.now());
                return user;
            }).map(userRepository::save);

            return new ResponseEntity<>(new LoginResponseDto(token), httpHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void checkForPendingPayment(Organization organization, User user) {
        paymentHistoryRepository.findOneByOrganizationIdAndPaymentStatus(organization.getId(), PaymentStatus.UNPAID)
                .filter(payment -> payment.getDueDate().isBefore(Instant.now()))
                .ifPresent(payment -> {
                    organization.setPaymentPending(true);
                    user.setAccountStatus(AccountStatus.PAYMENT_PENDING);
                    organizationRepository.save(organization);
                    userRepository.save(user);
                    CompletableFuture.runAsync(() -> paymentsService.notifyUserAboutAccountSuspension(user, payment, organization.getTimezone()));
                });
    }

}