package ru.gadjini.any2any.service.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.model.bot.api.object.InputFile;
import ru.gadjini.any2any.model.bot.api.object.InputMedia;
import ru.gadjini.any2any.service.telegram.TelegramBotApiService;
import ru.gadjini.any2any.service.telegram.TelegramMTProtoService;
import ru.gadjini.any2any.service.telegram.TelegramMediaService;

import java.io.File;

@Component
public class TelegramMediaServiceProvider {

    public static final int BOT_API_UPLOAD_FILE_LIMIT = 49 * 1024 * 1024;

    public static final int BOT_API_DOWNLOAD_FILE_LIMIT = 19 * 1024 * 1024;

    private TelegramBotApiService telegramBotApiService;

    private TelegramMTProtoService telegramMTProtoService;

    @Autowired
    public TelegramMediaServiceProvider(TelegramBotApiService telegramBotApiService, TelegramMTProtoService telegramMTProtoService) {
        this.telegramBotApiService = telegramBotApiService;
        this.telegramMTProtoService = telegramMTProtoService;
    }

    public TelegramMediaService getMediaService(InputMedia media) {
        return getMediaService(media.getFileId(), media.getFilePath());
    }

    public TelegramMediaService getMediaService(InputFile media) {
        return getMediaService(media.getFileId(), media.getFilePath());
    }

    public TelegramMediaService getStickerMediaService() {
        return telegramBotApiService;
    }

    public TelegramMediaService getMediaService(String fileId, String filePath) {
        if (StringUtils.isNotBlank(fileId)) {
            return telegramBotApiService;
        }
        File file = new File(filePath);

        if (file.length() < BOT_API_UPLOAD_FILE_LIMIT) {
            return telegramBotApiService;
        }

        return telegramMTProtoService;
    }

    public TelegramMediaService getDownloadMediaService(long fileSize) {
        if (fileSize > 0 && fileSize < BOT_API_DOWNLOAD_FILE_LIMIT) {
            return telegramBotApiService;
        }

        return telegramMTProtoService;
    }
}
