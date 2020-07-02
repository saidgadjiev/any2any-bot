package ru.gadjini.any2any.model.bot.api.method.send;

import ru.gadjini.any2any.model.bot.api.object.ParseMode;

public class HtmlMessage extends SendMessage {

    public HtmlMessage() {
        enableHtml(true);
    }

    public HtmlMessage(String chatId, String text) {
        super(chatId, text);
        enableHtml(true);
    }

    public HtmlMessage(Long chatId, String text) {
        super(chatId, text);
        enableHtml(true);
    }
}
