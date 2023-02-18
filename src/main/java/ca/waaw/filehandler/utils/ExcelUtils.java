package ca.waaw.filehandler.utils;

import ca.waaw.web.rest.errors.exceptions.application.MissingHeadersException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class ExcelUtils {

    private final static Logger log = LogManager.getLogger(ExcelUtils.class);

    /**
     * @param file Multipart Excel File
     * @return Workbook Object from Excel file
     */
    public static Workbook getWorkbook(InputStream file) {
        try {
            FileInputStream stream = (FileInputStream) file;
            return new XSSFWorkbook(stream);
        } catch (Exception e) {
            log.error("Exception while reading excel file", e);
        }
        return null;
    }

    /**
     * @param sheet Sheet object from Excel Workbook
     * @return List of all headers, i.e. first row values
     */
    public static List<String> getExcelSheetHeaders(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
        return headers;
    }

    /**
     * @param headers         The List of first row values from excel (Can be acquired using {@link #getExcelSheetHeaders(Sheet)}
     * @param requiredHeaders List of all required headers that cannot be left out.
     * @return List of all index for required values.
     */
    public static List<Integer> validateHeadersAndGetRequiredIndices(List<String> headers, String[] requiredHeaders) {
        List<String> missingHeaders = new ArrayList<>();
        List<Integer> requiredIndices = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean(false);
        Arrays.stream(requiredHeaders).forEach(requiredHeader -> {
            if (!headers.contains(requiredHeader)) {
                error.set(true);
                missingHeaders.add(requiredHeader);
            } else {
                requiredIndices.add(Arrays.asList(requiredHeaders).indexOf(requiredHeader));
            }
        });
        if (error.get()) {
            throw new MissingHeadersException("excel", missingHeaders.toArray(new String[0]));
        }
        return requiredIndices;
    }

    /**
     * @param cell     cell form Excel Row
     * @param dataType of the required return type
     * @param <T>      Return type
     * @return Required cell value in required data type
     */
    public static <T> T getCellValue(Cell cell, Class<T> dataType) {
        switch (cell.getCellType()) {
            case STRING:
                if (dataType.isEnum()) {
                    try {
                        return dataType.cast(dataType.getDeclaredMethod("valueOf", String.class)
                                .invoke(null, cell.getRichStringCellValue().getString().toUpperCase(Locale.ROOT)));
                    } catch (Exception e) {
                        log.error("Exception while casting to enum: {}", dataType, e);
                        return null;
                    }
                }
                return dataType.cast(cell.getRichStringCellValue().getString());
            case NUMERIC:
                if (dataType.isAssignableFrom(String.class))
                    return dataType.cast(String.valueOf(cell.getNumericCellValue()));
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

    /**
     * @param writableList list of an object array of data to be written in file
     * @param filename     file name for worksheet
     * @return Workbook object containing Excel sheet
     */
    public static XSSFWorkbook objectListToWorkbook(List<Object[]> writableList, String filename) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(filename);
        IntStream.range(0, writableList.size()).forEach(rowIndex -> {
            Row row = sheet.createRow(rowIndex);
            Object[] rowObject = writableList.get(rowIndex);
            IntStream.range(0, rowObject.length).forEach(cellIndex -> {
                Cell cell = row.createCell(cellIndex);
                Object cellObject = rowObject[cellIndex];
                if (cellObject instanceof String)
                    cell.setCellValue((String) cellObject);
                else if (cellObject instanceof Integer)
                    cell.setCellValue((Integer) cellObject);
                else if (cellObject instanceof Boolean)
                    cell.setCellValue((Boolean) cellObject);
            });
        });

        return workbook;
    }

}
