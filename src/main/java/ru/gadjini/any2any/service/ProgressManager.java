package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.message.TelegramMediaServiceProvider;

@Service
public class ProgressManager {

    public boolean isShowingProgress(long fileSize) {
        if (fileSize == 0) {
            return true;
        }
        return TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT < fileSize;
    }
}
