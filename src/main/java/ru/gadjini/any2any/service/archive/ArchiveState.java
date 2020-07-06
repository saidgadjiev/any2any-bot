package ru.gadjini.any2any.service.archive;

import ru.gadjini.any2any.model.Any2AnyFile;

import java.util.ArrayList;
import java.util.List;

public class ArchiveState {

    private String language;

    private int archiveCreatingMessageId;

    private List<Any2AnyFile> files = new ArrayList<>();

    public List<Any2AnyFile> getFiles() {
        return files;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getArchiveCreatingMessageId() {
        return archiveCreatingMessageId;
    }

    public void setArchiveCreatingMessageId(int archiveCreatingMessageId) {
        this.archiveCreatingMessageId = archiveCreatingMessageId;
    }
}
