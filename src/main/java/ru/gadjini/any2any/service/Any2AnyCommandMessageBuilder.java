package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.CommandMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.util.Locale;

@Service
public class Any2AnyCommandMessageBuilder implements CommandMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public Any2AnyCommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append("/").append(CommandNames.START_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.IMAGE_EDITOR_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.CONVERT_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.QUERIES_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.QUERIES_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.RENAME_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.RENAME_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.OCR_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.OCR_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.UNZIP_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.UNZIP_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.ARCHIVE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.ARCHIVE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(FileUtilsCommandNames.FORMATS_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}
