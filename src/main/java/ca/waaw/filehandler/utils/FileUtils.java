package ca.waaw.filehandler.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

public class FileUtils {

    public static String getFileExtension(MultipartFile file) {
        return Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf('.') + 1);
    }

}
