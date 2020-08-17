package ru.gadjini.any2any.service.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.any2any.exception.botapi.TelegramApiException;
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.model.ApiResponse;
import ru.gadjini.any2any.model.bot.api.method.IsChatMember;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.DeleteMessage;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageReplyMarkup;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.property.BotApiProperties;

import java.io.IOException;

@Service
public class TelegramBotApiService {

    private final BotApiProperties botApiProperties;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public TelegramBotApiService(BotApiProperties botApiProperties, ObjectMapper objectMapper) {
        this.botApiProperties = botApiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }


    public Boolean isChatMember(IsChatMember isChatMember) {
        try {
            HttpEntity<IsChatMember> request = new HttpEntity<>(isChatMember);
            String result = restTemplate.postForObject(getUrl(IsChatMember.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> apiResponse = objectMapper.readValue(result, new TypeReference<>() {
                });

                if (apiResponse.getOk()) {
                    return apiResponse.getResult();
                } else {
                    throw new TelegramApiRequestException(String.valueOf(isChatMember.getUserId()), "Error is chat member", apiResponse);
                }
            } catch (IOException e) {
                throw new TelegramApiException("Unable to deserialize response(" + result + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(String.valueOf(isChatMember.getUserId()), e.getMessage(), e);
        }
    }

    public Boolean sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            HttpEntity<AnswerCallbackQuery> request = new HttpEntity<>(answerCallbackQuery);
            String response = restTemplate.postForObject(getUrl(AnswerCallbackQuery.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
                });

                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(null, "Error answering callback query", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(null, "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiException(e);
        }
    }

    public Message sendMessage(SendMessage sendMessage) {
        try {
            HttpEntity<SendMessage> request = new HttpEntity<>(sendMessage);
            String response = restTemplate.postForObject(getUrl(HtmlMessage.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendMessage.getChatId(), "Error sending message", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendMessage.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendMessage.getChatId(), e.getMessage(), e);
        }
    }

    public void editReplyMarkup(EditMessageReplyMarkup editMessageReplyMarkup) {
        try {
            HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(editMessageReplyMarkup);
            String response = restTemplate.postForObject(getUrl(EditMessageReplyMarkup.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Error editing message reply markup", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), e.getMessage(), e);
        }
    }

    public void editMessageText(EditMessageText editMessageText) {
        try {
            HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
            String response = restTemplate.postForObject(getUrl(EditMessageText.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageText.getChatId(), "Error editing message text", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageText.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageText.getChatId(), e.getMessage(), e);
        }
    }

    public void editMessageCaption(EditMessageCaption editMessageCaption) {
        try {
            HttpEntity<EditMessageCaption> request = new HttpEntity<>(editMessageCaption);
            String response = restTemplate.postForObject(getUrl(EditMessageCaption.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Error editing message caption", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageCaption.getChatId(), e.getMessage(), e);
        }
    }

    public Boolean deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
            String response = restTemplate.postForObject(getUrl(DeleteMessage.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(deleteMessage.getChatId(), "Error deleting message", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(deleteMessage.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(deleteMessage.getChatId(), e.getMessage(), e);
        }
    }

    private String getUrl(String method) {
        return botApiProperties.getApi() + method;
    }
}
