package ru.gadjini.any2any.service.archive;

import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;

public class ArchiveState {

    private String language;

    private List<MessageMedia> files = new ArrayList<>();

    private String archiveFilePath;

    private Format archiveType;

    public List<MessageMedia> getFiles() {
        return files;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getArchiveFilePath() {
        return archiveFilePath;
    }

    public void setArchiveFilePath(String archiveFilePath) {
        this.archiveFilePath = archiveFilePath;
    }

    public Format getArchiveType() {
        return archiveType;
    }

    public void setArchiveType(Format archiveType) {
        this.archiveType = archiveType;
    }
}
