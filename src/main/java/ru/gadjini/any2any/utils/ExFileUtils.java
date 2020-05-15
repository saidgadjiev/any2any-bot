package ru.gadjini.any2any.utils;

import java.io.File;
import java.util.List;

public class ExFileUtils {

    public static void list(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if(fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    list(file.getAbsolutePath(), files);
                }
            }
        }
    }
}
