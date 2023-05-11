package ca.waaw.security.jwt;

import ca.waaw.config.applicationconfig.AppSecurityConfig;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.security.SecurityUtils;
import io.jsonwebtoken.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Component
@Configuration
@AllArgsConstructor
public class TokenProvider {

    private final Logger log = LogManager.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private static final String ACCOUNT_STATUS_KEY = "account_status";

    private final AppSecurityConfig appSecurityConfig;

    public String createToken(Authentication authentication, Boolean rememberMe, AccountStatus accountStatus) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + (this.appSecurityConfig.getJwtRememberMeTokenValidityInSeconds() * 1000));
        } else {
            validity = new Date(now + this.appSecurityConfig.getJwtDefaultTokenValidityInSeconds() * 1000);
        }

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(ACCOUNT_STATUS_KEY, accountStatus.toString())
                .signWith(SignatureAlgorithm.HS512, appSecurityConfig.getJwtSecret())
                .setExpiration(validity)
                .compact();
    }

    public String updateUsernameOrStatusInToken(String username, AccountStatus accountStatus) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String authorities = securityContext.getAuthentication().getAuthorities()
                .stream().map(Object::toString).collect(Collectors.joining(","));
        Date validity = SecurityUtils.getCurrentUserJWT()
                .map(this::getExpirationDateFromToken).orElse(null);
        AccountStatus accountStatusOld = SecurityUtils.getCurrentUserJWT()
                .map(this::checkAccountStatus).orElse(null);
        String usernameOld = SecurityUtils.getCurrentUserLogin().orElse(null);

        return Jwts.builder()
                .setSubject(StringUtils.isNotEmpty(username) ? username : usernameOld)
                .claim(AUTHORITIES_KEY, authorities)
                .claim(ACCOUNT_STATUS_KEY, accountStatus != null ? accountStatus.toString() : Objects.requireNonNull(accountStatusOld).toString())
                .signWith(SignatureAlgorithm.HS512, appSecurityConfig.getJwtSecret())
                .setExpiration(validity)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(appSecurityConfig.getJwtSecret())
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    //validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (SignatureException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT token.");
            log.trace("Invalid JWT token trace: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
            log.trace("Unsupported JWT token trace: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e.getMessage());
        }
        return false;
    }

    //retrieve username from jwt token
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    //retrieve expiration date from jwt token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    //Check the associated account status
    public AccountStatus checkAccountStatus(String token) {
        Object status = getClaimFromToken(token, (claims) -> claims.get(ACCOUNT_STATUS_KEY)).toString();
        if (status != null) {
            return AccountStatus.valueOf(status.toString());
        }
        return null;
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    //for retrieving any information from token we will need the secret key
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(appSecurityConfig.getJwtSecret()).parseClaimsJws(token).getBody();
    }

    //check if the token has expired
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}
