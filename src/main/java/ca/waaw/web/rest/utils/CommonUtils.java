package ca.waaw.web.rest.utils;

import ca.waaw.enumration.Authority;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

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
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", locale);
        return bundle.getString(property);
    }

    /**
     * @param firstname firstname
     * @param lastname  lastname
     * @return combined string without any white spaces
     */
    public static String combineFirstAndLastName(String firstname, String lastname) {
        assert (StringUtils.isNotEmpty(firstname));
        return StringUtils.isNotEmpty(lastname) ? firstname + " " + lastname : firstname;
    }

    public static String combinePhoneNumber(String countryCode, String mobile) {
        if (StringUtils.isEmpty(countryCode) && StringUtils.isEmpty(mobile)) return null;
        return StringUtils.isNotEmpty(countryCode) ? countryCode + " " + mobile : mobile;
    }

    public static void validateStringInEnum(Class<?extends Enum<?>> enumClass, String value, String field) {
        if (Stream.of(enumClass.getEnumConstants()).map(Enum::name).noneMatch(name -> name.equalsIgnoreCase(value))) {
            throw new BadRequestException("Invalid value for the field", field);
        }
    }

    public static <S> S logMessageAndReturnObject(S object, String logType, Class<?> logLocation, String message,
                                                  Object... messageParams) {
        Logger log = LogManager.getLogger(logLocation);
        switch (logType.toLowerCase(Locale.ROOT)) {
            case "debug":
                log.debug(message, messageParams);
                break;
            case "error":
                log.error(message, messageParams);
                break;
            case "info":
                log.info(message, messageParams);
                break;
        }
        return object;
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
