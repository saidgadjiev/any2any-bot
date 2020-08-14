package ru.gadjini.any2any.service.message;

import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;

public interface MediaMessageService {
    EditMediaResult editMessageMedia(EditMessageMedia editMediaContext);

    void sendSticker(SendSticker sendSticker);

    SendFileResult sendDocument(SendDocument sendDocumentContext);

    SendFileResult sendPhoto(SendPhoto sendPhoto);

    void sendVideo(SendVideo sendVideo);

    void sendAudio(SendAudio sendAudio);

    MediaType getMediaType(String fileId);

    void sendFile(long chatId, String fileId);

    void sendFile(long chatId, String fileId, String caption);
}
