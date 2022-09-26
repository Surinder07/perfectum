package ca.waaw.filehandler;

import ca.waaw.filehandler.enumration.PojoToMap;
import ca.waaw.filehandler.utils.FileToPojoUtils;
import ca.waaw.filehandler.utils.FileUtils;
import ca.waaw.web.rest.errors.exceptions.UnsupportedFileFormatException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class FileHandler {

    private final static Logger log = LogManager.getLogger(FileToPojoUtils.class);

    private final FileConfig fileConfig;

    /**
     * @param file        Multipart file to be read
     * @param cls         class of object to be mapped
     * @param missingData an empty list to collect all missing data information
     * @param pojoToMap   pojo type that is to be mapped
     * @param <T>         return type
     * @return List of all mapped object from the file provided
     */
    public <T> List<T> readExcelOrCsv(InputStream file, String fileName, Class<T> cls, List<T> missingData,
                                      PojoToMap pojoToMap) {
        String fileExtension = FileUtils.getFileExtension(fileName);
        List<T> result;
        if (Arrays.asList(fileConfig.getFormatsAllowed().getExcel()).contains(fileExtension)) {
            result = new ArrayList<>();
            FileToPojoUtils.excelFileToObject(file, result, getRequiredHeaders(pojoToMap), cls,
                    getPojoTemplates(pojoToMap), missingData);
        } else if (Arrays.asList(fileConfig.getFormatsAllowed().getCsv()).contains(fileExtension)) {
            result = FileToPojoUtils.csvToObject(file, fileName, cls, getRequiredHeaders(pojoToMap),
                    getPojoTemplates(pojoToMap), missingData);
        } else {
            log.error("File {} is of unsupported format: {}", fileName, fileExtension);
            throw new UnsupportedFileFormatException(ArrayUtils.addAll(fileConfig.getFormatsAllowed().getExcel(),
                    fileConfig.getFormatsAllowed().getCsv()));
        }
        return result;
    }

    /**
     * @param pojoToMap pojo type that is to be mapped
     * @return String[] of required headers for the object to be mapped
     */
    private String[] getRequiredHeaders(PojoToMap pojoToMap) {
        if (pojoToMap.equals(PojoToMap.HOLIDAY)) {
            return fileConfig.getRequiredFields().getHolidays();
        }
        return null;
    }

    /**
     * @param pojoToMap pojo type that is to be mapped
     * @return Map for csv headers and pojo fields
     */
    private Map<String, String> getPojoTemplates(PojoToMap pojoToMap) {
        if (pojoToMap.equals(PojoToMap.HOLIDAY)) {
            return fileConfig.getPojoTemplates().getHolidays();
        }
        return null;
    }

}