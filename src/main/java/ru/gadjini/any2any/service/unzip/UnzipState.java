package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnzipState {

    private String archivePath;

    private Format archiveType;

    private Map<Integer, String> files = new HashMap<>();

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

    public Map<Integer, String> getFiles() {
        return files;
    }
}
