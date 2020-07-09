package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.Collections;
import java.util.List;

public interface UnzipDevice {

    void unzip(int userId, String in, String out);

    default List<ZipFileHeader> getZipFiles(String zipFile) {
        return Collections.emptyList();
    }

    default String unzip(String fileHeader, String archivePath, String dir) {
        return null;
    }

    boolean accept(Format zipFormat);
}
