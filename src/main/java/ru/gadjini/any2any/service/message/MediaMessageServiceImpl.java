package ru.gadjini.any2any.service.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.ParseMode;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;

@Service
@Qualifier("media")
public class MediaMessageServiceImpl implements MediaMessageService {

    private FileService fileService;

    private TelegramService telegramService;

    @Autowired
    public MediaMessageServiceImpl(FileService fileService,
                                   TelegramService telegramService) {
        this.fileService = fileService;
        this.telegramService = telegramService;
    }

    @Override
    public EditMediaResult editMessageMedia(EditMessageMedia editMessageMedia) {
        if (StringUtils.isNotBlank(editMessageMedia.getMedia().getCaption())) {
            editMessageMedia.getMedia().setParseMode(ParseMode.HTML);
        }
        Message message = telegramService.editMessageMedia(editMessageMedia);

        return new EditMediaResult(fileService.getFileId(message));
    }

    @Override
    public SendFileResult sendDocument(SendDocument sendDocument) {
        if (StringUtils.isNotBlank(sendDocument.getCaption())) {
            sendDocument.setParseMode(ParseMode.HTML);
        }

        Message message = telegramService.sendDocument(sendDocument);

        return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
    }

    @Override
    public SendFileResult sendPhoto(SendPhoto sendPhoto) {
        Message message = telegramService.sendPhoto(sendPhoto);

        return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
    }

    @Override
    public void sendVideo(SendVideo sendVideo) {
        if (StringUtils.isNotBlank(sendVideo.getCaption())) {
            sendVideo.setParseMode(ParseMode.HTML);
        }
        telegramService.sendVideo(sendVideo);
    }

    @Override
    public void sendAudio(SendAudio sendAudio) {
        if (StringUtils.isNotBlank(sendAudio.getCaption())) {
            sendAudio.setParseMode(ParseMode.HTML);
        }

        telegramService.sendAudio(sendAudio);
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return telegramService.getMediaType(fileId);
    }

    @Override
    public void sendFile(long chatId, String fileId) {
        sendFile(chatId, fileId, null);
    }

    @Override
    public void sendFile(long chatId, String fileId, String caption) {
        MediaType mediaType = getMediaType(fileId);

        switch (mediaType) {
            case PHOTO:
                sendPhoto(new SendPhoto(chatId, fileId).setCaption(caption));
                break;
            case VIDEO:
                sendVideo(new SendVideo(chatId, fileId).setCaption(caption));
                break;
            case AUDIO:
                sendAudio(new SendAudio(chatId, fileId).setCaption(caption));
                break;
            default:
                sendDocument(new SendDocument(chatId, fileId).setCaption(caption));
                break;
        }
    }

    @Override
    public void sendSticker(SendSticker sendSticker) {
        telegramService.sendSticker(sendSticker);
    }
}
