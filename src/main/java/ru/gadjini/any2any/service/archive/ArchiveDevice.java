package ru.gadjini.any2any.service.archive;

import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.List;

public interface ArchiveDevice {

    void zip(List<String> files, String out);

    default String rename(String archive, String fileHeader, String newFileName) {
        return null;
    }

    boolean accept(Format format);
}
