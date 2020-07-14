package ru.gadjini.any2any.bot.command.api;

import ru.gadjini.any2any.model.bot.api.object.Message;

public interface BotCommand {
    String COMMAND_INIT_CHARACTER = "/";

    void processMessage(Message message);

    String getCommandIdentifier();
}
