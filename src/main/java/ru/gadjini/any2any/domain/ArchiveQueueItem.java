package ru.gadjini.any2any.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.domain.WorkQueueItem;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class ArchiveQueueItem extends WorkQueueItem {

    public static final String NAME = "archive_queue";

    public static final String FILES = "files";

    public static final String TYPE = "type";

    public static final String ARCHIVE_FILE_PATH = "archive_file_path";

    public static final String DOWNLOADED_FILES_COUNT = "downloaded_files_count";

    private List<TgFile> files;

    private Format type;

    private String archiveFilePath;

    private int downloadedFilesCount;

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

    public String getArchiveFilePath() {
        return archiveFilePath;
    }

    public void setArchiveFilePath(String archiveFilePath) {
        this.archiveFilePath = archiveFilePath;
    }

    public int getDownloadedFilesCount() {
        return downloadedFilesCount;
    }

    public void setDownloadedFilesCount(int downloadedFilesCount) {
        this.downloadedFilesCount = downloadedFilesCount;
    }

    @Override
    public long getSize() {
        return files.stream().mapToLong(TgFile::getSize).sum();
    }
}
