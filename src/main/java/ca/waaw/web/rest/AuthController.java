package ca.waaw.web.rest;

import ca.waaw.dto.userdtos.LoginDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.repository.UserRepository;
import ca.waaw.security.jwt.JWTFilter;
import ca.waaw.security.jwt.TokenProvider;
import ca.waaw.web.rest.errors.exceptions.AuthenticationException;
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

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "Auth", description = "Authentication API")
public class AuthController {

    private final Logger log = LogManager.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    @Operation(summary = "Authenticate login password to get a jwt token")
    @SwaggerBadRequest
    @ApiResponse(responseCode = "401", description = "Authentication Failed", content = @Content)
    @ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDto.class))})
    @PostMapping("/v1/unAuth/authenticate")
    public ResponseEntity<LoginResponseDto> authenticate(@Valid @RequestBody LoginDto loginDto) {
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginDto.getLogin(), loginDto.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            throw new AuthenticationException();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw e;
        }

        final String token = tokenProvider.createToken(authentication, loginDto.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        // Update last login to current time
        userRepository.findOneByUsernameOrEmail(loginDto.getLogin(), loginDto.getLogin())
                .map(user -> {
                    user.setLastLogin(Instant.now());
                    return user;
                }).map(userRepository::save);

        return new ResponseEntity<>(new LoginResponseDto(token), httpHeaders, HttpStatus.OK);
    }

}