package ca.waaw.filehandler.utils;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class FileToPojoUtils {

    private final static Logger log = LogManager.getLogger(FileToPojoUtils.class);

    /**
     * Will populate the passed list with objects mapped with all values from excel
     *
     * @param file            Multipart file to be read
     * @param listToPopulate  List of the object that will be populated with result
     * @param requiredHeaders List of headers that cannot be null
     * @param cls             Class of object that result will be cast to
     * @param pojoTemplate    Map of column name to pojo field
     * @param missingFields an empty set to collect header for any missing data
     * @param <T>             populating Object
     */
    public static <T> void excelFileToObject(InputStream file, List<T> listToPopulate, String[] requiredHeaders,
                                             Class<T> cls, Map<String, String> pojoTemplate, Set<String> missingFields) {
        Workbook workbook = ExcelUtils.getWorkbook(file);
        assert workbook != null;
        workbook.forEach(sheet -> {
            List<Integer> requiredIndices = ExcelUtils.validateHeadersAndGetRequiredIndices(ExcelUtils.getExcelSheetHeaders(sheet),
                    requiredHeaders);
            List<T> resultList = FileToPojoUtils.excelSheetToObject(cls, sheet, pojoTemplate, requiredIndices,
                    missingFields);
            listToPopulate.addAll(resultList);
        });
    }

    /**
     * @param cls             class of populating object
     * @param sheet           sheet containing data
     * @param pojoTemplate    Map of column name to pojo field
     * @param requiredIndices List of all index on which data cannot be null
     * @param missingFields an empty set to collect header for any missing data
     * @param <T>             populating Object
     * @return List of given object
     */
    public static <T> List<T> excelSheetToObject(Class<T> cls, Sheet sheet, Map<String, String> pojoTemplate,
                                                 List<Integer> requiredIndices, Set<String> missingFields) {
        List<T> results = new ArrayList<>();
        List<String> headers = ExcelUtils.getExcelSheetHeaders(sheet);
        log.info("Processing excel sheet: {}", sheet.getSheetName());
        log.info("Data per sheet, count: {}", sheet.getLastRowNum());
        IntStream.range(1, sheet.getLastRowNum() + 1).forEach(rowIndex -> {
            Row row = sheet.getRow(rowIndex);
            try {
                MutableBoolean skipRow = new MutableBoolean(false);
                T result = cls.getDeclaredConstructor().newInstance();
                IntStream.range(0, row.getPhysicalNumberOfCells()).forEach(cellIndex -> {
                    if (headers.get(cellIndex) != null) {
                        String fieldName = pojoTemplate.get(headers.get(cellIndex));
                        Field field = getField(cls, fieldName);
                        Object fieldValue = ExcelUtils.getCellValue(row.getCell(cellIndex), field.getType());
                        if (requiredIndices.contains(cellIndex) && fieldValue == null) {
                            skipRow.setTrue();
                            missingFields.add(headers.get(cellIndex));
                        }
                        try {
                            field.set(result, fieldValue);
                        } catch (Exception e) {
                            log.error("Error while populating {} object, {} field", cls, fieldName, e);
                        }
                    }
                });
                if (skipRow.isFalse()) results.add(result);
            } catch (Exception e) {
                log.error("Exception while creating new instance of class: {}", cls, e);
            }
        });
        return results;
    }

    /**
     * @param file            Multipart file to be read
     * @param cls             class of populating object
     * @param requiredHeaders List of headers that cannot be null
     * @param pojoTemplate    Map of column name to pojo field
     * @param missingFields an empty set to collect header for any missing data
     * @param <T>             populating Object
     * @return List of populated object
     */
    public static <T> List<T> csvToObject(InputStream file, String fileName, Class<T> cls, String[] requiredHeaders,
                                          Map<String, String> pojoTemplate, Set<String> missingFields) {
        String[] headers = CsvUtils.getCsvHeaders(file, fileName);
        if (headers != null) {
            log.info("Processing csv file: {}", fileName);
            List<Integer> requiredIndices = CsvUtils.validateHeadersAndGetRequiredIndices(headers, requiredHeaders);
            try {
                List<T> results = new ArrayList<>();
                CSVParser csvParser = CsvUtils.getCsvParser(file, headers);
                assert csvParser != null;
                List<CSVRecord> records = csvParser.getRecords();
                log.info("Records found in CSV, count: {}", records.size());
                if (records.size() > 0) {
                    IntStream.range(0, records.size()).forEach(rowIndex -> {
                        if (headers[rowIndex] != null) {
                            MutableBoolean skipRow = new MutableBoolean(false);
                            try {
                                CSVRecord record = records.get(rowIndex);
                                T result = cls.getDeclaredConstructor().newInstance();
                                IntStream.range(0, headers.length).forEach(valueIndex -> {
                                    String fieldName = pojoTemplate.get(headers[valueIndex]);
                                    Field field = getField(cls, fieldName);
                                    Object fieldValue = CsvUtils.getCellValue(record.get(headers[valueIndex]), field.getType());
                                    if (requiredIndices.contains(valueIndex) && fieldValue == null) {
                                        skipRow.setTrue();
                                        missingFields.add(headers[valueIndex]);
                                    }
                                    try {
                                        field.set(result, fieldValue);
                                    } catch (Exception e) {
                                        log.error("Error while populating {} object, {} field", cls, fieldName, e);
                                    }
                                });
                                if (skipRow.isFalse()) results.add(result);
                            } catch (Exception e) {
                                log.error("Exception while creating new instance of class: {}", cls, e);
                            }
                        }
                    });
                }
                return results;
            } catch (Exception e) {
                log.error("Exception while reading csv file", e);
            }
        } else {
            log.error("Could not extract headers for CSV file: {}", fileName);
        }
        return null;
    }

    /**
     * @param cls       Class of object containing field
     * @param fieldName name of the field
     * @param <T>       Object containing field
     * @return Field object
     */
    public static <T> Field getField(Class<T> cls, String fieldName) {
        Field field = null;
        try {
            field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (Exception e) {
            log.error("Exception while getting field for pojo: {}", cls, e);
        }
        return field;
    }

}
