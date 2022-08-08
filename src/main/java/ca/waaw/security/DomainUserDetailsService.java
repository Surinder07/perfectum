package ca.waaw.security;

import ca.waaw.domain.User;
import ca.waaw.enumration.EntityStatus;
import ca.waaw.repository.UserRepository;
import ca.waaw.web.rest.errors.exceptions.UserAccountDisabledException;
import ca.waaw.web.rest.errors.exceptions.UserNotActivatedException;
import ca.waaw.web.rest.errors.exceptions.UsernameNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Authenticate a user from the database.
 */
@AllArgsConstructor
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private final Logger log = LogManager.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        Optional<User> userByEmailFromDatabase = userRepository.findOneByEmailAndDeleteFlag(lowercaseLogin, false);
        return userByEmailFromDatabase.map(user -> createSpringSecurityUser(lowercaseLogin, user))
                .orElseGet(() -> {
                    Optional<User> userByLoginFromDatabase = userRepository.findOneByUsernameAndDeleteFlag(lowercaseLogin, false);
                    return userByLoginFromDatabase.map(user -> createSpringSecurityUser(lowercaseLogin, user))
                            .orElseThrow(() -> new UsernameNotFoundException(lowercaseLogin));
                });
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        if (user.getStatus().equals(EntityStatus.PENDING)) {
            throw new UserNotActivatedException(lowercaseLogin);
        } else if (user.getStatus().equals(EntityStatus.SUSPENDED)) {
            throw new UserAccountDisabledException();
        }
        List<GrantedAuthority> grantedAuthorities = Collections.singletonList(new SimpleGrantedAuthority(user.getAuthority().name()));

        return new org.springframework.security.core.userdetails.User(user.getUsername(),
                user.getPasswordHash(), grantedAuthorities);
    }
}
