package ru.gadjini.any2any.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class ArchiveQueueItem extends QueueItem {

    public static final String NAME = "archive_queue";

    public static final String FILES = "files";

    public static final String TYPE = "type";

    public static final String TOTAL_FILE_SIZE = "total_file_size";

    private long totalFileSize;

    private List<TgFile> files;

    private Format type;

    public List<TgFile> getFiles() {
        return files;
    }

    public void setFiles(List<TgFile> files) {
        this.files = files;
    }

    public Format getType() {
        return type;
    }

    public void setType(Format type) {
        this.type = type;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    @Override
    public long getSize() {
        return getTotalFileSize();
    }
}
