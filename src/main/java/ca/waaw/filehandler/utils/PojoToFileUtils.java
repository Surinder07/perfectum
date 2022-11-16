package ca.waaw.filehandler.utils;

import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.mapper.ReportsMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class PojoToFileUtils {

    private final static Logger log = LogManager.getLogger(PojoToFileUtils.class);

    /**
     * @param writableList Use {@link ReportsMapper} class to get the list of object array
     * @param filename     file name for the workbook
     * @return byte array for the file
     */
    public static ByteArrayResource convertObjectToListOfWritableObject(List<Object[]> writableList, String filename, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (format.equalsIgnoreCase("xls")) {
                XSSFWorkbook workbook = ExcelUtils.objectListToWorkbook(writableList, filename);
                workbook.write(out);
            } else if (format.equalsIgnoreCase("csv")) {
                OutputStreamWriter writer = new OutputStreamWriter(out);
                CsvUtils.objectListToWorkbook(writableList, writer);
            } else {
                log.error("Format ({}) not allowed while creating report", format);
                throw new BadRequestException(format + "format not available");
            }
            byte[] dataArray = out.toByteArray();
            return new ByteArrayResource(dataArray);
        } catch (Exception e) {
            throw new BadRequestException(""); //TODO change exception type
        }
    }

}
