package ca.waaw.web.rest;

import ca.waaw.domain.User;
import ca.waaw.dto.userdtos.LoginDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.enumration.Authority;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.jwt.JWTFilter;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.web.rest.errors.PaymentErrorVM;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.TrialExpiredException;
import ca.waaw.web.rest.utils.APIConstants;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = APIConstants.TagNames.auth, description = APIConstants.TagDescription.auth)
public class AuthController {

    private final Logger log = LogManager.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    @Operation(description = APIConstants.ApiDescription.Auth.authentication)
    @SwaggerBadRequest
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class))})
    @ApiResponse(responseCode = "401", description = APIConstants.ErrorDescription.authentication, content = @Content)
    @ApiResponse(responseCode = "402", description = APIConstants.ErrorDescription.trialOver,
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PaymentErrorVM.class))})
    @PostMapping(APIConstants.ApiEndpoints.Auth.authentication)
    public ResponseEntity<LoginResponseDto> authenticate(@Valid @RequestBody LoginDto loginDto) {
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

        Optional<User> userEntity = userRepository.findOneByUsernameOrEmail(loginDto.getLogin(), loginDto.getLogin());

        userEntity.ifPresent(user -> organizationRepository.findOneByIdAndDeleteFlag(user.getOrganizationId(), false)
                .map(organization -> {
                    if (organization.getCreatedDate().isBefore(Instant.now().minus(organization.getTrialDays(), ChronoUnit.DAYS))
                            && !user.getAuthority().equals(Authority.SUPER_USER)) {
                        throw new TrialExpiredException(user.getId(), user.getAuthority());
                    }
                    return null;
                })
        );

        final String token = tokenProvider.createToken(authentication, loginDto.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        // Update last login to current time
        userEntity.map(user -> {
            user.setLastLogin(Instant.now());
            return user;
        }).map(userRepository::save);

        return new ResponseEntity<>(new LoginResponseDto(token), httpHeaders, HttpStatus.OK);
    }

}