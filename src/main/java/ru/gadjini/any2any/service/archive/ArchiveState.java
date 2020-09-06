package ru.gadjini.any2any.service.archive;

import ru.gadjini.telegram.smart.bot.commons.model.Any2AnyFile;

import java.util.ArrayList;
import java.util.List;

public class ArchiveState {

    private String language;

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
}
