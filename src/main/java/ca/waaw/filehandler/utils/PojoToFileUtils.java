package ca.waaw.filehandler.utils;

import ca.waaw.web.rest.errors.exceptions.BadRequestException;
import ca.waaw.mapper.ReportsMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PojoToFileUtils {

    /**
     * @param writableList Use {@link ReportsMapper} class to get the list of object array
     * @param filename     file name for the workbook
     * @return byte array for the file
     */
    public static ByteArrayResource convertObjectToListOfWritableObject(List<Object[]> writableList, String filename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XSSFWorkbook workbook = ExcelUtils.pojoToWorkbook(writableList, filename);
            workbook.write(out);
            byte[] dataArray = out.toByteArray();
            return new ByteArrayResource(dataArray);
        } catch (Exception e) {
            throw new BadRequestException(""); //TODO change exception type
        }
    }

}
