package ca.waaw.filehandler.utils;

import ca.waaw.web.rest.errors.exceptions.application.MissingHeadersException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExcelUtils {

    private final static Logger log = LogManager.getLogger(ExcelUtils.class);

    public static Workbook getWorkbook(MultipartFile file) {
        try {
            FileInputStream stream = (FileInputStream) file.getInputStream();
            return new XSSFWorkbook(stream);
        } catch (Exception e) {
            log.error("Exception while reading excel file", e);
        }
        return null;
    }

    public static List<String> getExcelSheetHeaders(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
        return headers;
    }

    public static void validateHeaders(List<String> headers, String[] requiredHeaders) {
        List<String> missingHeaders = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean(false);
        Arrays.stream(requiredHeaders).forEach(requiredHeader -> {
            if (!headers.contains(requiredHeader)) {
                error.set(true);
                missingHeaders.add(requiredHeader);
            }
        });
        if (error.get()) {
            throw new MissingHeadersException("excel", missingHeaders.toArray(new String[0]));
        }
    }

    public static <T> T getCellValue(Cell cell, Class<T> dataType) {
        switch (cell.getCellType()) {
            case STRING:
                if (dataType.isEnum()) {
                    try {
                        return dataType.cast(dataType.getDeclaredMethod("valueOf", String.class)
                                .invoke(null, cell.getRichStringCellValue().getString()));
                    } catch (Exception ignored) { }
                }
                return dataType.cast(cell.getRichStringCellValue().getString());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return dataType.cast(cell.getDateCellValue().toInstant());
                } else {
                    if (Float.class.equals(dataType)) {
                        return dataType.cast((float) cell.getNumericCellValue());
                    } else if (Integer.class.equals(dataType)) {
                        return dataType.cast((int) cell.getNumericCellValue());
                    } else if (Long.class.equals(dataType)) {
                        return dataType.cast((long) cell.getNumericCellValue());
                    } else {
                        return dataType.cast(cell.getNumericCellValue());
                    }
                }
            case BOOLEAN:
                return dataType.cast(cell.getBooleanCellValue());
            case FORMULA:
                return dataType.cast(cell.getCellFormula());
            default:
                return null;
        }
    }

}
