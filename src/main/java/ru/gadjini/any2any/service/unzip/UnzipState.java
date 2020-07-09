package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnzipState {

    private String archivePath;

    private Format archiveType;

    private Map<Integer, ZipFileHeader> files = new HashMap<>();

    private Map<Integer, String> filesCache = new HashMap<>();

    private int chooseFilesMessageId;

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


    public int getChooseFilesMessageId() {
        return chooseFilesMessageId;
    }

    public void setChooseFilesMessageId(int chooseFilesMessageId) {
        this.chooseFilesMessageId = chooseFilesMessageId;
    }

    public Map<Integer, String> getFilesCache() {
        return filesCache;
    }

    public void setFilesCache(Map<Integer, String> filesCache) {
        this.filesCache = filesCache;
    }
}
