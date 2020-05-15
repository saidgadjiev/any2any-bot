package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.command.CommandExecutor;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.keyboard.CurrReplyKeyboard;

import java.util.Locale;

@Service
public class Any2AnyBotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Any2AnyBotService.class);

    private MessageService messageService;

    private CommandExecutor commandExecutor;

    private LocalisationService localisationService;

    private UserService userService;

    private CommandNavigator commandNavigator;

    private CurrReplyKeyboard replyKeyboardService;

    @Autowired
    public Any2AnyBotService(MessageService messageService, CommandExecutor commandExecutor,
                             LocalisationService localisationService, UserService userService,
                             CommandNavigator commandNavigator, CurrReplyKeyboard replyKeyboardService) {
        this.messageService = messageService;
        this.commandExecutor = commandExecutor;
        this.localisationService = localisationService;
        this.userService = userService;
        this.commandNavigator = commandNavigator;
        this.replyKeyboardService = replyKeyboardService;
    }

    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                if (restoreCommand(
                        update.getMessage().getChatId(),
                        update.getMessage().hasText() ? update.getMessage().getText().trim() : null
                )) {
                    return;
                }
                String text = getText(update.getMessage());
                if (commandExecutor.isKeyboardCommand(update.getMessage().getChatId(), text)) {
                    if (isOnCurrentMenu(update.getMessage().getChatId(), text)) {
                        commandExecutor.executeKeyBoardCommand(update.getMessage(), text);

                        return;
                    }
                } else if (commandExecutor.isBotCommand(update.getMessage())) {
                    if (commandExecutor.executeBotCommand(update.getMessage())) {
                        return;
                    } else {
                        messageService.sendMessage(
                                new SendMessageContext(
                                        update.getMessage().getChatId(),
                                        localisationService.getMessage(MessagesProperties.MESSAGE_UNKNOWN_COMMAND, userService.getLocale(update.getMessage().getFrom().getId()))));
                        return;
                    }
                }
                commandExecutor.processNonCommandUpdate(update.getMessage(), text);
            } else if (update.hasCallbackQuery()) {
                commandExecutor.executeCallbackCommand(update.getCallbackQuery());
            }
        } catch (UserException ex) {
            LOGGER.error(ex.getMessage());
            messageService.sendMessage(new SendMessageContext(TgMessage.getChatId(update), ex.getMessage()).webPagePreview(true));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            TgMessage tgMessage = TgMessage.from(update);
            messageService.sendErrorMessage(tgMessage.getChatId(), userService.getLocale(tgMessage.getUser().getId()));
        }
    }

    private String getText(Message message) {
        if (message.hasText()) {
            return message.getText().trim();
        }

        return "";
    }

    private boolean restoreCommand(long chatId, String command) {
        if (StringUtils.isNotBlank(command) && command.startsWith(BotCommand.COMMAND_INIT_CHARACTER + CommandNames.START_COMMAND)) {
            return false;
        }
        if (commandNavigator.isEmpty(chatId)) {
            commandNavigator.zeroRestore(chatId, (NavigableBotCommand) commandExecutor.getBotCommand(CommandNames.START_COMMAND));
            Locale locale = userService.getLocale((int) chatId);
            messageService.sendBotRestartedMessage(chatId, replyKeyboardService.getMainMenu(chatId, locale), locale);

            return true;
        }

        return false;
    }

    private boolean isOnCurrentMenu(long chatId, String commandText) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardService.getCurrentReplyKeyboard(chatId);

        if (replyKeyboardMarkup == null) {
            return true;
        }

        for (KeyboardRow keyboardRow : replyKeyboardMarkup.getKeyboard()) {
            if (keyboardRow.stream().anyMatch(keyboardButton -> keyboardButton.getText().equals(commandText))) {
                return true;
            }
        }

        return false;
    }
}
