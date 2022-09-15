package ca.waaw.filehandler.utils;

import ca.waaw.web.rest.errors.exceptions.application.MissingHeadersException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CsvUtils {

    private final static Logger log = LogManager.getLogger(CsvUtils.class);

    /**
     * @param file    Multipart file for CSV
     * @param headers headers present in the file
     * @return {@link CSVParser} for given CSV
     */
    public static CSVParser getCsvParser(MultipartFile file, String[] headers) {
        try {
            return CSVParser.parse(file.getInputStream(), Charset.defaultCharset(),
                    CSVFormat.DEFAULT
                            .builder().setAllowMissingColumnNames(false)
                            .setHeader(headers)
                            .setSkipHeaderRecord(true).build());
        } catch (Exception e) {
            log.error("Exception while reading excel file", e);
        }
        return null;
    }

    /**
     * @param file Multipart File for CSV file
     * @return String[] containing the first row elements
     */
    public static String[] getCsvHeaders(MultipartFile file) {
        try (Scanner scanner = new Scanner(file.getInputStream())) {
            scanner.useDelimiter(",");
            return scanner.nextLine().split(",");
        } catch (Exception e) {
            log.error("Exception while reading CSV Headers for file: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * @param headers         The List of first row values from excel (Can be acquired using {@link #getCsvHeaders(MultipartFile)})
     * @param requiredHeaders List of all required headers that cannot be left out.
     * @return List of all index for required values.
     */
    public static List<Integer> validateHeadersAndGetRequiredIndices(String[] headers, String[] requiredHeaders) {
        List<String> missingHeaders = new ArrayList<>();
        List<Integer> requiredIndices = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean(false);
        Arrays.stream(requiredHeaders).forEach(requiredHeader -> {
            if (!Arrays.asList(headers).contains(requiredHeader)) {
                error.set(true);
                missingHeaders.add(requiredHeader);
            } else {
                requiredIndices.add(Arrays.asList(requiredHeaders).indexOf(requiredHeader));
            }
        });
        if (error.get()) {
            throw new MissingHeadersException("csv", missingHeaders.toArray(new String[0]));
        }
        return requiredIndices;
    }

    /**
     * @param value    String value read from csv
     * @param dataType required type the value needs to be converted
     * @param <T>      Return type
     * @return converted value
     */
    public static <T> T getCellValue(String value, Class<T> dataType) {
        if (dataType.equals(String.class)) {
            return dataType.cast(value);
        } else if (dataType.equals(Integer.class)) {
            return dataType.cast(Integer.parseInt(value));
        } else if (dataType.equals(Long.class)) {
            return dataType.cast(Long.parseLong(value));
        } else if (dataType.equals(Float.class)) {
            return dataType.cast(Float.parseFloat(value));
        } else if (dataType.equals(Double.class)) {
            return dataType.cast(Double.parseDouble(value));
        } else if (dataType.equals(Boolean.class)) {
            return dataType.cast(Boolean.valueOf(value));
        } else if (dataType.isEnum()) {
            try {
                return dataType.cast(dataType.getDeclaredMethod("valueOf", String.class)
                        .invoke(null, value.toUpperCase(Locale.ROOT)));
            } catch (Exception e) {
                log.error("Exception while casting to enum: {}", dataType, e);
                return null;
            }
        }
        return null;
    }

}
