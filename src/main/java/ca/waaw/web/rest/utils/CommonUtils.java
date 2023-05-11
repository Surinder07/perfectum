package ca.waaw.web.rest.utils;

import ca.waaw.domain.user.User;
import ca.waaw.dto.PaginationDto;
import ca.waaw.enumration.user.AccountStatus;
import ca.waaw.enumration.user.Authority;
import ca.waaw.security.SecurityUtils;
import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.web.rest.errors.exceptions.UnauthorizedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
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
     * @param source String to change
     * @return split string form underscore and capitalize first alphabet of each word
     */
    public static String capitalizeString(String source) {
        StringJoiner joiner = new StringJoiner(" ");
        Arrays.stream(source.toLowerCase().split("_"))
                .map(StringUtils::capitalize)
                .forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * @param enumClass Enum class to match string with
     * @param value     value to be matched with enum
     * @param field     field that will be shown in exception thrown
     */
    public static void validateStringInEnum(Class<? extends Enum<?>> enumClass, String value, String field) {
        if (value == null) return;
        if (Stream.of(enumClass.getEnumConstants()).map(Enum::name).noneMatch(name -> name.equalsIgnoreCase(value))) {
            throw new BadRequestException("Invalid value for the field", field);
        }
    }

    /**
     * @param pattern pattern to match
     * @param value   value to be matched
     * @param field   field for which value is validated
     */
    public static void validateRegexString(String pattern, String value, String field) {
        if (!Pattern.matches(pattern, value)) {
            throw new BadRequestException("Field " + field + " contains invalid value");
        }
    }

    /**
     * @param commaSeparatedString comma separated string to be converted to list
     * @return List containing all comma separated values
     */
    public static List<String> commaSeparatedStringToList(String commaSeparatedString) {
        if (commaSeparatedString == null) return null;
        return Arrays.asList(commaSeparatedString.replaceAll(", ", ",").split(","));
    }

    /**
     * @param existingString existing string if any
     * @param stringToAdd    list of string to add
     * @return comma separated string for all values
     */
    public static String combineListToCommaSeparatedString(String existingString, List<String> stringToAdd) {
        StringJoiner joiner = new StringJoiner(",");
        if (existingString != null) {
            joiner.add(existingString);
        }
        stringToAdd.forEach(joiner::add);
        return joiner.toString();
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
     * @param page         Page object containing all data
     * @param mapper       mapper function to convert to dto list
     * @param secondObject second argument for mapper function
     * @param <M>          Class type for Page entity
     * @param <S>          Class type for DTO response
     * @return PaginationDto containing list of dto and page info
     */
    public static <M, T, S> PaginationDto getPaginationResponse(Page<M> page, BiFunction<M, T, S> mapper,
                                                                T secondObject) {
        List<S> data = page.getContent().stream()
                .map(obj -> mapper.apply(obj, secondObject))
                .collect(Collectors.toList());
        return PaginationDto.builder()
                .totalEntries((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .data(data)
                .build();
    }

    /**
     * @param page         Page object containing all data
     * @param mapper       mapper function to convert to dto list
     * @param secondObject second argument for mapper function
     * @param thirdObject  third argument for mapper function
     * @return PaginationDto containing list of dto and page info
     */
    public static <M, T, R, S> PaginationDto getPaginationResponse(Page<M> page, TriFunction<M, T, R, S> mapper,
                                                                   T secondObject, R thirdObject) {
        List<S> data = page.getContent().stream()
                .map(obj -> mapper.apply(obj, secondObject, thirdObject))
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
        String newNumber = String.valueOf(Integer.parseInt(currentCustomId.substring(1)) + 1);
        String nameSuffix = StringUtils.leftPad(newNumber, numericLength, '0');
        return currentCustomId.charAt(0) + nameSuffix;
    }

    /**
     * @param resource resource to be sent through api
     * @param filename filename to be included in the headers
     * @return Response Entity with the file
     */
    public static ResponseEntity<Resource> byteArrayResourceToResponse(ByteArrayResource resource, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    /**
     * @param users a list of users
     * @return count of employees who logged-in during last 48 hours
     */
    public static int getActiveEmployeesFromList(List<User> users) {
        return (int) users.stream()
                .filter(user -> !user.getAuthority().equals(Authority.ADMIN))
                .filter(user -> user.getAccountStatus().equals(AccountStatus.PAID_AND_ACTIVE)).count();
    }

    /**
     * Throws exception if date is not yyyy-MM-dd format
     *
     * @param dates All dates that require verifying
     */
    public static void validateDateFormat(String[]... dates) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        AtomicBoolean error = new AtomicBoolean(false);
        List<String> errorFields = new ArrayList<>();
        Arrays.stream(dates).forEach(date -> {
            try {
                sdf.parse(date[0]);
            } catch (ParseException e) {
                error.set(true);
                errorFields.add(date[1]);
            }
        });
        if (error.get()) {
            throw new BadRequestException("Please enter valid date values (yyyy-MM-dd)", errorFields.toArray(new String[0]));
        }
    }

    /**
     * @param value         the value we need validated
     * @param field         field name to be sent in error
     * @param valuesToMatch allowed values for the validating value
     */
    public static void matchAndValidateStringValues(String value, String field, String... valuesToMatch) {
        if (Arrays.stream(valuesToMatch).noneMatch(val -> val.equalsIgnoreCase(value))) {
            throw new BadRequestException("Invalid value", field);
        }
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
