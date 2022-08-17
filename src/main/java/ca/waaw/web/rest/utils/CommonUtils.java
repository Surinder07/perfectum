package ca.waaw.web.rest.utils;

import ca.waaw.enumration.Authority;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.ResourceBundle;

public class CommonUtils {

    /**
     * Checks if user has given authorities or else throw {@link UnauthorizedException}
     *
     * @param authorities authorities to check for
     */
    public static void checkRoleAuthorization(Authority... authorities) {
        if (!SecurityUtils.isCurrentUserInRole(authorities)) {
            throw new UnauthorizedException();
        }
    }

    /**
     * Will fetch the property from {@code messages.properties} resource bundle
     *
     * @param property property key
     * @param locale   language needed for property
     * @return Value for the property key
     */
    public static String getPropertyFromMessagesResourceBundle(String property, Locale locale) {
        if (locale == null) locale = Locale.ENGLISH;
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString(property);
    }

    /**
     * {@link #generateRandomKey()} will generate a random 20 character key that we are mostly using for
     * activation, invite, etc. type of links
     * Number of character for key is defined in {@link #DEF_COUNT}
     */
    public final static class Random {
        private static final int DEF_COUNT = 20;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private Random() {
        }

        public static String generateRandomKey() {
            return RandomStringUtils.random(DEF_COUNT, 0, 0, true, true, null, SECURE_RANDOM);
        }

        static {
            SECURE_RANDOM.nextBytes(new byte[64]);
        }
    }

}
