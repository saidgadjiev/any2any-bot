package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.converter.impl.FormatService;

import java.util.Comparator;
import java.util.Locale;

@Service
public class FileService {

    private LocalisationService localisationService;

    private FormatService formatService;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Autowired
    public FileService(LocalisationService localisationService, FormatService formatService) {
        this.localisationService = localisationService;
        this.formatService = formatService;
    }

    public String getFileId(Message message) {
        if (message.hasDocument()) {
            return message.getDocument().getFileId();
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();

            return photoSize.getFileId();
        } else if (message.hasVideo()) {
            return message.getVideo().getFileId();
        } else if (message.hasAudio()) {
            return message.getAudio().getFileId();
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();

            return sticker.getFileId();
        }

        return null;
    }

    public Any2AnyFile getFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = new Any2AnyFile();

        if (message.hasDocument()) {
            any2AnyFile.setFileName(message.getDocument().getFileName());
            any2AnyFile.setFileId(message.getDocument().getFileId());
            any2AnyFile.setMimeType(message.getDocument().getMimeType());

            return any2AnyFile;
        } else if (message.hasPhoto()) {
            any2AnyFile.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".jpg");
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            any2AnyFile.setFileId(photoSize.getFileId());
            any2AnyFile.setMimeType("image/jpeg");

            return any2AnyFile;
        } else if (message.hasVideo()) {
            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
            String extension = formatService.getExt(message.getVideo().getMimeType());
            if (StringUtils.isNotBlank(extension)) {
                fileName += extension;
            } else {
                fileName += "mp4";
            }
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setFileId(message.getVideo().getFileId());

            return any2AnyFile;
        } else if (message.hasAudio()) {
            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
            String extension = formatService.getExt(message.getAudio().getMimeType());
            fileName += extension;
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setFileId(message.getAudio().getFileId());
            any2AnyFile.setMimeType(message.getAudio().getMimeType());

            return any2AnyFile;
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();
            any2AnyFile.setFileId(sticker.getFileId());
            String fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".";
            fileName += sticker.getAnimated() ? "tgs" : "webp";
            any2AnyFile.setFileName(fileName);
            any2AnyFile.setMimeType(sticker.getAnimated() ? null : "image/webp");

            return any2AnyFile;
        }
        LOGGER.debug("File can't be extracted from message " + TgMessage.from(message));

        return null;
    }
}
