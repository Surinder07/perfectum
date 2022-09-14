package ca.waaw.filehandler;

import ca.waaw.filehandler.utils.ExcelUtils;
import ca.waaw.filehandler.utils.FileToPojoUtils;
import ca.waaw.filehandler.utils.FileUtils;
import ca.waaw.web.rest.errors.exceptions.UnsupportedFileFormatException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class FileHandler {

    private final FileConfig fileConfig;

    public <T> List<T> readExcelOrCsv(MultipartFile file, Class<T> cls, MutableBoolean missingData) {
        String fileExtension = FileUtils.getFileExtension(file);
        List<T> result = new ArrayList<>();
        if (Arrays.asList(fileConfig.getFormatsAllowed().getExcel()).contains(fileExtension)) {
            Workbook workbook = ExcelUtils.getWorkbook(file);
            assert workbook != null;
            workbook.forEach(sheet -> {
                ExcelUtils.validateHeaders(ExcelUtils.getExcelSheetHeaders(sheet), fileConfig.getRequiredFields().getHolidays());
                List<T> resultList = FileToPojoUtils.excelSheetToObject(cls, sheet, fileConfig.getPojoTemplates().getHolidays(),
                        missingData);
                result.addAll(resultList);
            });
        } else if (Arrays.asList(fileConfig.getFormatsAllowed().getCsv()).contains(fileExtension)) {

        } else {
            throw new UnsupportedFileFormatException(ArrayUtils.addAll(fileConfig.getFormatsAllowed().getExcel(),
                    fileConfig.getFormatsAllowed().getCsv()));
        }
        return result;
    }
}