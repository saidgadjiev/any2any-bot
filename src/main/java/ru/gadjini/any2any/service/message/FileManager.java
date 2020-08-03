package ru.gadjini.any2any.service.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;

@Service
public class FileManager {

    private TelegramService telegramService;

    @Autowired
    public FileManager(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    public void downloadFileByFileId(String fileId, SmartTempFile outputFile) {
        telegramService.downloadFileByFileId(fileId, outputFile);
    }

    public boolean cancelDownloading(String fileId) {
        return telegramService.cancelDownloading(fileId);
    }

    public void cancelDownloads() {
        telegramService.cancelDownloads();
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        telegramService.restoreFileIfNeed(filePath, fileId);
    }
}
