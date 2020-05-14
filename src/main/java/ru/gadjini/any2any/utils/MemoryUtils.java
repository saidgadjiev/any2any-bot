package ru.gadjini.any2any.utils;

import org.apache.commons.io.FileUtils;

public class MemoryUtils {

    public static String humanReadableByteCount(long bytes) {
        return FileUtils.byteCountToDisplaySize(bytes);
    }
}
