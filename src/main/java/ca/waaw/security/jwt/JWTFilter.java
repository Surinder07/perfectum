package ca.waaw.security.jwt;

import ca.waaw.enumration.Authority;
import ca.waaw.web.rest.errors.exceptions.TrialExpiredException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        log.info("Requested endpoint: {}", request.getRequestURI());
        try {
            final String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
            // JWT Token is in the form "Bearer token". Remove Bearer word and get
            // only the Token
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                String jwtToken = requestTokenHeader.substring(7);
                if (tokenProvider.checkTrialExpiry() && !request.getRequestURI().equals(String
                        .format("/api%s", env.getProperty("api.endpoints.user-organization.getUserDetails")))) {
                    throw new TrialExpiredException(Authority.ADMIN);
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

}
