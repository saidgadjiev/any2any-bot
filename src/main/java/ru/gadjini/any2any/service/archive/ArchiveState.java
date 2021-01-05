package ru.gadjini.any2any.service.archive;

import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

import java.util.ArrayList;
import java.util.List;

public class ArchiveState {

    private String language;

    private List<MessageMedia> files = new ArrayList<>();

    public List<MessageMedia> getFiles() {
        return files;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
