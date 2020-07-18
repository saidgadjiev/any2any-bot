package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnzipState {

    private String archivePath;

    private Format archiveType;

    private int unzipJobId;

    private final Map<Integer, ZipFileHeader> files = new HashMap<>();

    private final Map<Integer, String> renamed = new HashMap<>();

    private final Map<Integer, String> filesCache = new HashMap<>();

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public Set<Integer> filesIds() {
        return files.keySet();
    }

    public Format getArchiveType() {
        return archiveType;
    }

    public void setArchiveType(Format archiveType) {
        this.archiveType = archiveType;
    }

    public Map<Integer, ZipFileHeader> getFiles() {
        return files;
    }

    public Map<Integer, String> getFilesCache() {
        return filesCache;
    }

    public int getUnzipJobId() {
        return unzipJobId;
    }

    public void setUnzipJobId(int unzipJobId) {
        this.unzipJobId = unzipJobId;
    }

    public Map<Integer, String> getRenamed() {
        return renamed;
    }
}
