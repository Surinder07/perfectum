package ca.waaw.filehandler.utils;

public class FileUtils {

    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

}
