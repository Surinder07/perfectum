package ca.waaw.filehandler.utils;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class FileToPojoUtils {

    private final static Logger log = LogManager.getLogger(FileToPojoUtils.class);

    /**
     * @param cls          class of populating object
     * @param sheet        sheet containing data
     * @param pojoTemplate Map of column name to pojo field
     * @param missingData  initially false boolean value that will be marked true if some data is missing,
     *                     this will be used later to send notification in case of missing data.
     * @param <T>          populating Object
     * @return List of given object
     */
    public static <T> List<T> excelSheetToObject(Class<T> cls, Sheet sheet, Map<String, String> pojoTemplate,
                                                 MutableBoolean missingData) {
        List<T> results = new ArrayList<>();
        List<String> headers = ExcelUtils.getExcelSheetHeaders(sheet);
        log.info("Processing excel sheet: {}", sheet.getSheetName());
        log.info("Data per sheet, count: {}", sheet.getLastRowNum());
        IntStream.range(1, sheet.getLastRowNum() + 1).forEach(rowIndex -> {
            Row row = sheet.getRow(rowIndex);
            try {
                T result = cls.getDeclaredConstructor().newInstance();
                IntStream.range(0, row.getPhysicalNumberOfCells()).forEach(cellIndex -> {
                    if (headers.get(cellIndex) != null) {
                        String fieldName = pojoTemplate.get(headers.get(cellIndex));
                        Field field = getField(cls, fieldName);
                        Object fieldValue = ExcelUtils.getCellValue(row.getCell(cellIndex), field.getType());
                        try {
                            field.set(result, fieldValue);
                        } catch (Exception e) {
                            log.error("Error while populating {} object, {} field", cls, fieldName, e);
                            missingData.setValue(true);
                        }
                    }
                });
                results.add(result);
            } catch (Exception e) {
                missingData.setValue(true);
                log.error("Exception while creating new instance of class: {}", cls, e);
            }
        });
        return results;
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
