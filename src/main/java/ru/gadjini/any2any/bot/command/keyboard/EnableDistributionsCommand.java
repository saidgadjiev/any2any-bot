package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.job.DistributionJob;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class EnableDistributionsCommand implements KeyboardBotCommand {

    private Set<String> names = new HashSet<>();

    private final LocalisationService localisationService;

    private UserService userService;

    private DistributionJob distributionJob;

    private MessageService messageService;

    @Autowired
    public EnableDistributionsCommand(LocalisationService localisationService, UserService userService,
                                      DistributionJob distributionJob, @Qualifier("messagelimits") MessageService messageService) {
        this.localisationService = localisationService;
        this.userService = userService;
        this.distributionJob = distributionJob;
        this.messageService = messageService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.ENABLE_DISTRIBUTIONS_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        if (!userService.isAdmin((int) chatId)) {
            return false;
        }
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        distributionJob.checkDistributions();

        messageService.sendMessage(new SendMessage(message.getChatId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_DISTRIBUTIONS_ENABLED, userService.getLocaleOrDefault(message.getFromUser().getId()))));
        return false;
    }
}
