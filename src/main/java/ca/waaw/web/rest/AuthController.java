package ca.waaw.web.rest;

import ca.waaw.domain.User;
import ca.waaw.dto.userdtos.LoginDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.enumration.Authority;
import ca.waaw.repository.OrganizationRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.jwt.JWTFilter;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.web.rest.errors.ErrorVM;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
import ca.waaw.web.rest.errors.exceptions.application.TrialExpiredException;
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
@Tag(name = "${api.swagger.groups.auth}")
public class AuthController {

    private final Logger log = LogManager.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final OrganizationRepository organizationRepository;

    @SwaggerBadRequest
    @Operation(description = "${api.description.authentication}")
    @PostMapping("${api.endpoints.authentication}")
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class))})
    @ApiResponse(responseCode = "401", description = "${api.swagger.error-description.authentication}", content = @Content)
    @ApiResponse(responseCode = "402", description = "${api.swagger.error-description.trial-over}",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorVM.class))})
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

        boolean isTrialOver = userEntity
                .flatMap(user -> organizationRepository.findOneByIdAndDeleteFlagAndTrialDaysNot(user.getOrganizationId(), false, 0)
                        .map(organization -> {
                            if (!user.getAuthority().equals(Authority.SUPER_USER) &&
                                    organization.getCreatedDate().isBefore(Instant.now().minus(organization.getTrialDays(), ChronoUnit.DAYS))) {
                                return true;
                            }
                            return null;
                        })
                ).orElse(false);
        userEntity.ifPresent(user -> {
            if (isTrialOver && !user.getAuthority().equals(Authority.ADMIN)) {
                throw new TrialExpiredException(user.getAuthority());
            }
        });

        final String token = tokenProvider.createToken(authentication, loginDto.isRememberMe(), isTrialOver);
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