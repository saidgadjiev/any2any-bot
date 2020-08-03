package ru.gadjini.any2any.service.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.filter.TelegramLimitsFilter;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Service
public class UnzipMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public UnzipMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String buildUnzipProgressMessage(UnzipStep unzipStep, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);

        switch (unzipStep) {
            case DOWNLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIPPING_STEP, locale) + "\n" +
            case UNZIPPING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIPPING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " " + formatter + "\n";
            default:

        }
    }

    public FilesMessage getFilesList(Map<Integer, ZipFileHeader> files, int limit, int offset, Locale locale) {
        StringBuilder message = new StringBuilder();

        for (Iterator<Map.Entry<Integer, ZipFileHeader>> iterator = files.entrySet().stream().skip(offset).iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, ZipFileHeader> entry = iterator.next();
            StringBuilder fileHeaderStr = new StringBuilder();
            fileHeaderStr.append(entry.getKey()).append(") ").append(entry.getValue().getPath());
            if (entry.getValue().getSize() != 0) {
                fileHeaderStr.append(" (").append(MemoryUtils.humanReadableByteCount(entry.getValue().getSize())).append(")");
            }
            String finalMessage = localisationService.getMessage(
                    MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                    new Object[]{message.toString() + fileHeaderStr.toString()},
                    locale
            );
            if (finalMessage.length() > TelegramLimitsFilter.TEXT_LENGTH_LIMIT) {
                return new FilesMessage(localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{message.toString()},
                        locale
                ), limit, offset);
            } else {
                message.append(fileHeaderStr);
            }
            if (iterator.hasNext()) {
                message.append("\n");
            }
            ++limit;
        }

        return new FilesMessage(localisationService.getMessage(
                MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                new Object[]{message.toString()},
                locale
        ), limit, offset);
    }

    public String getFilesList(Collection<ZipFileHeader> files) {
        StringBuilder message = new StringBuilder();
        int i = 1;
        for (Iterator<ZipFileHeader> iterator = files.iterator(); iterator.hasNext(); ) {
            ZipFileHeader zipFileHeader = iterator.next();
            message.append(i++).append(") ").append(zipFileHeader.getPath());
            if (zipFileHeader.getSize() != 0) {
                message.append(" (").append(MemoryUtils.humanReadableByteCount(zipFileHeader.getSize())).append(")");
            }
            if (iterator.hasNext()) {
                message.append("\n");
            }
        }

        return message.toString();
    }

    public static class FilesMessage {

        private String message;

        private int limit;

        private int offset;

        private FilesMessage(String message, int limit, int offset) {
            this.message = message;
            this.limit = limit;
            this.offset = offset;
        }

        public int getLimit() {
            return limit;
        }

        public String getMessage() {
            return message;
        }

        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return "FilesMessage{" +
                    "message='\n" + message + '\'' +
                    ", limit=" + limit +
                    ", offset=" + offset +
                    '}';
        }
    }
}
