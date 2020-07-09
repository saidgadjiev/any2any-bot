package ru.gadjini.any2any.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.any2any.exception.DownloadCanceledException;
import ru.gadjini.any2any.exception.botapi.TelegramApiException;
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.ApiResponse;
import ru.gadjini.any2any.model.bot.api.method.CancelDownloading;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.GetFile;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.property.TelegramProperties;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

    private final Map<String, SmartTempFile> downloading = new ConcurrentHashMap<>();

    private final TelegramProperties telegramProperties;

    private TempFileService fileService;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public TelegramService(TelegramProperties telegramProperties, TempFileService fileService, ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public Boolean sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            HttpEntity<AnswerCallbackQuery> request = new HttpEntity<>(answerCallbackQuery);
            String response = restTemplate.postForObject(getUrl(AnswerCallbackQuery.METHOD), request, String.class);
            ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
            });

            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(null, "Error answering callback query", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiException("Unable to deserialize response", e);
        }
    }

    public Message sendMessage(SendMessage sendMessage) {
        try {
            HttpEntity<SendMessage> request = new HttpEntity<>(sendMessage);
            String response = restTemplate.postForObject(getUrl(HtmlMessage.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(sendMessage.getChatId(), "Error sending message", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(sendMessage.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message editReplyMarkup(EditMessageReplyMarkup editMessageReplyMarkup) {
        try {
            HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(editMessageReplyMarkup);
            String response = restTemplate.postForObject(getUrl(EditMessageReplyMarkup.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Error editing message reply markup", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message editMessageText(EditMessageText editMessageText) {
        try {
            HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
            String response = restTemplate.postForObject(getUrl(EditMessageText.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(editMessageText.getChatId(), "Error editing message text", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(editMessageText.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message editMessageCaption(EditMessageCaption editMessageCaption) {
        try {
            HttpEntity<EditMessageCaption> request = new HttpEntity<>(editMessageCaption);
            String response = restTemplate.postForObject(getUrl(EditMessageCaption.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Error editing message caption", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message editMessageMedia(EditMessageMedia editMessageMedia) {
        try {
            HttpEntity<EditMessageMedia> request = new HttpEntity<>(editMessageMedia);
            String response = restTemplate.postForObject(getUrl(EditMessageMedia.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Error editing message media", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message sendSticker(SendSticker sendSticker) {
        try {
            HttpEntity<SendSticker> request = new HttpEntity<>(sendSticker);
            String response = restTemplate.postForObject(getUrl(SendSticker.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(sendSticker.getChatId(), "Error sending sticker", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(sendSticker.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Boolean deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
            String response = restTemplate.postForObject(getUrl(DeleteMessage.METHOD), request, String.class);
            ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(deleteMessage.getChatId(), "Error deleting message", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(deleteMessage.getChatId(), "Unable to deserialize response", e);
        }
    }

    public Message sendDocument(SendDocument sendDocument) {
        try {
            HttpEntity<SendDocument> request = new HttpEntity<>(sendDocument);
            String response = restTemplate.postForObject(getUrl(SendDocument.METHOD), request, String.class);
            ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
            });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException(sendDocument.getChatId(), "Error sending document", result);
            }
        } catch (IOException | RestClientException e) {
            throw new TelegramApiRequestException(sendDocument.getChatId(), "Unable to deserialize response", e);
        }
    }

    public void downloadFileByFileId(String fileId, SmartTempFile outputFile) {
        downloading.put(fileId, outputFile);
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            LOGGER.debug("Start downloadFileByFileId({})", fileId);

            HttpEntity<GetFile> request = new HttpEntity<>(new GetFile(fileId, outputFile.getAbsolutePath(), outputFile.isDeleteParentDir()));
            restTemplate.postForObject(getUrl(GetFile.METHOD), request, Void.class);

            if (!downloading.containsKey(fileId)) {
                throw new DownloadCanceledException();
            }

            stopWatch.stop();
            LOGGER.debug("Finish downloadFileByFileId({}, {}, {})", fileId, MemoryUtils.humanReadableByteCount(outputFile.length()), stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            downloading.remove(fileId);
        }
    }

    public SmartTempFile downloadFileByFileId(String fileId, String ext) {
        SmartTempFile smartTempFile = fileService.createTempFile0(fileId, ext);
        try {
            downloadFileByFileId(fileId, smartTempFile);
        } catch (Exception ex) {
            smartTempFile.smartDelete();
            throw ex;
        }

        return smartTempFile;
    }

    public void cancelDownloading(String fileId) {
        try {
            if (downloading.containsKey(fileId)) {
                HttpEntity<CancelDownloading> request = new HttpEntity<>(new CancelDownloading(fileId));
                restTemplate.postForObject(getUrl(CancelDownloading.METHOD), request, Void.class);
            }
        } finally {
            downloading.remove(fileId);
        }
    }

    public void cancelDownloads() {
        try {
            for (Map.Entry<String, SmartTempFile> entry : downloading.entrySet()) {
                HttpEntity<CancelDownloading> request = new HttpEntity<>(new CancelDownloading(entry.getKey()));
                restTemplate.postForObject(getUrl(CancelDownloading.METHOD), request, Void.class);
            }
        } finally {
            downloading.clear();
        }
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        if (!new File(filePath).exists()) {
            downloadFileByFileId(fileId, new SmartTempFile(new File(filePath), false));
            LOGGER.debug("File restored({}, {})", fileId, filePath);
        }
    }

    private String getUrl(String method) {
        return telegramProperties.getApi() + method;
    }
}
