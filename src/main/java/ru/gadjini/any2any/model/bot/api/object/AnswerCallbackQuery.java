package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnswerCallbackQuery {

    public static final String METHOD = "answercallbackquery";

    private static final String CALLBACKQUERYID_FIELD = "callback_query_id";

    private static final String TEXT_FIELD = "text";

    @JsonProperty(CALLBACKQUERYID_FIELD)
    private String callbackQueryId;

    @JsonProperty(TEXT_FIELD)
    private String text;

    public AnswerCallbackQuery() {}

    public AnswerCallbackQuery(String callbackQueryId, String text) {
        this.callbackQueryId = callbackQueryId;
        this.text = text;
    }

    public String getCallbackQueryId() {
        return this.callbackQueryId;
    }

    public String getText() {
        return this.text;
    }

    public void setCallbackQueryId(String callbackQueryId) {
        this.callbackQueryId = callbackQueryId;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "AnswerCallbackQuery{" +
                "callbackQueryId='" + callbackQueryId + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
