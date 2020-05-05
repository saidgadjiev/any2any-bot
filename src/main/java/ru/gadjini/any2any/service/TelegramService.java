package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gadjini.any2any.property.BotProperties;

import java.io.File;

@Service
public class TelegramService extends DefaultAbsSender {

    private final BotProperties botProperties;

    public TelegramService(BotProperties botProperties, DefaultBotOptions botOptions) {
        super(botOptions);
        this.botProperties = botProperties;
    }

    public String getBotToken() {
        return botProperties.getToken();
    }

    public File downloadFileByFileId(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
            return downloadFile(file);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
