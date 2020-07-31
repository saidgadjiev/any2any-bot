package ru.gadjini.any2any.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class FileUtils2 {

    private FileUtils2() {
    }

    public static boolean isExpired(File file, int days) {
        try {
            FileTime creationTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");

            LocalDateTime creationDateTime = creationTime.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

            long between = ChronoUnit.DAYS.between(creationDateTime, LocalDateTime.now());
            if (between > days) {
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
