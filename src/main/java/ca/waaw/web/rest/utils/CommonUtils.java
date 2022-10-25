package ca.waaw.web.rest.utils;

import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.Authority;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /**
     * @param countryCode country code
     * @param mobile      mobile
     * @return combined string without any white spaces
     */
    public static String combinePhoneNumber(String countryCode, String mobile) {
        if (StringUtils.isEmpty(countryCode) && StringUtils.isEmpty(mobile)) return null;
        return StringUtils.isNotEmpty(countryCode) ? countryCode + " " + mobile : mobile;
    }

    /**
     * @param enumClass Enum class to match string with
     * @param value     value to be matched with enum
     * @param field     field that will be shown in exception thrown
     */
    public static void validateStringInEnum(Class<? extends Enum<?>> enumClass, String value, String field) {
        if (Stream.of(enumClass.getEnumConstants()).map(Enum::name).noneMatch(name -> name.equalsIgnoreCase(value))) {
            throw new BadRequestException("Invalid value for the field", field);
        }
    }


    /**
     * @param commaSeparatedString comma separated string to be converted to list
     * @return List containing all comma separated values
     */
    public static List<String> commaSeparatedStringToList(String commaSeparatedString) {
        return Arrays.asList(commaSeparatedString.replace("[", "").replace("]", "")
                .replaceAll(", ", ",").split(","));
    }

    /**
     * @param object        Object to return after logging
     * @param logType       log type (info, debug, etc..)
     * @param logLocation   Class to show with logging
     * @param message       message to log
     * @param messageParams params to be passed in message
     * @param <S>           Class type for return object
     * @return Object passed as parameter
     */
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
     * @param page   Page object containing all data
     * @param mapper mapper function to convert to dto list
     * @param <M>    Class type for Page entity
     * @param <S>    Class type for DTO response
     * @return PaginationDto containing list of dto and page info
     */
    public static <M, S> PaginationDto getPaginationResponse(Page<M> page, Function<M, S> mapper) {
        List<S> data = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return PaginationDto.builder()
                .totalEntries((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .data(data)
                .build();
    }

    /**
     * @param page   Page object containing all data
     * @param mapper mapper function to convert to dto list
     * @param timezone timezone to convert the dates to
     * @param <M>    Class type for Page entity
     * @param <S>    Class type for DTO response
     * @return PaginationDto containing list of dto and page info
     */
    public static <M, S> PaginationDto getPaginationResponse(Page<M> page, BiFunction<M, String, S> mapper,
                                                             String timezone) {
        List<S> data = page.getContent().stream()
                .map(obj -> mapper.apply(obj, timezone))
                .collect(Collectors.toList());
        return PaginationDto.builder()
                .totalEntries((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .data(data)
                .build();
    }

    /**
     * @param currentCustomId current id
     * @param numericLength   numeric length in the id
     * @return next id in the sequence
     */
    public static String getNextCustomId(String currentCustomId, int numericLength) {
        String newNumber = String.valueOf(Integer.parseInt(currentCustomId.substring(3)) + 1);
        String nameSuffix = StringUtils.leftPad(newNumber, numericLength
                - newNumber.length(), '0');
        return currentCustomId.substring(0, 3) + nameSuffix;
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
