package ca.waaw.security.jwt;

import ca.waaw.config.applicationconfig.AppSecurityConfig;
import ca.waaw.enumration.ErrorCodes;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
@Component
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final Logger log = LogManager.getLogger(JWTFilter.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenProvider tokenProvider;

    private final Environment env;

    private final AppSecurityConfig appSecurityConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        log.info("Requested endpoint: {}", request.getRequestURI());
        try {
            final String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
            // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
            if (!isUnAuthUrl(request.getRequestURI()) && requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                String jwtToken = requestTokenHeader.substring(7);
                ErrorCodes error = checkLoginPermission(request, jwtToken);
                if (error != null) {
                    respondToErrorIfAny(error, response);
                    return;
                }
                Authentication authentication = tokenProvider.getAuthentication(jwtToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to get JWT Token");
        } catch (ExpiredJwtException e) {
            log.error("JWT Token has expired");
        }
        chain.doFilter(request, response);
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private ErrorCodes checkLoginPermission(HttpServletRequest request, String jwtToken) {
        switch (tokenProvider.checkAccountStatus(jwtToken)) {
            case PAYMENT_PENDING:
                if (!request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.payment-apis.confirmPayment"))) &&
                        !request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.payment-apis.createPaymentIntent"))) &&
                        !request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.payment-apis.getPendingPayment"))) &&
                        !request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.payment-apis.createSetupIntent"))))
                    return ErrorCodes.WE_003;
                break;
            case PAYMENT_INFO_PENDING:
                if (!request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.user.completePaymentInfo"))) &&
                        !request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.payment-apis.createSetupIntent"))))
                    return ErrorCodes.WE_002;
                break;
            case PROFILE_PENDING:
                if (!request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.user.completeRegistration"))) &&
                        !request.getRequestURI().equals(String.format("/api%s", env.getProperty("api.endpoints.user.validatePromoCode"))))
                    return ErrorCodes.WE_001;
                break;
        }
        return null;
    }

    private void respondToErrorIfAny(ErrorCodes errorCode, HttpServletResponse response) throws IOException {
        String sb = "{ " +
                "\"waawErrorCode\": \"" + errorCode.name() + "\"," +
                "\"message\": \"" + errorCode.value + "\"" +
                "}";
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(sb);
    }

    private boolean isUnAuthUrl(String requestedUri) {
        List<PathMatcher> pathMatchers = Arrays.stream(appSecurityConfig.getUnAuthUrlPatterns())
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                .collect(Collectors.toList());
        return pathMatchers.stream().anyMatch(pattern -> pattern.matches(Paths.get(requestedUri)));
    }

}
