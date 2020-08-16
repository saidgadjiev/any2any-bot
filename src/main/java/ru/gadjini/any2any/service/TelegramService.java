package ru.gadjini.any2any.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
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
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.CancelDownloading;
import ru.gadjini.any2any.model.bot.api.method.IsChatMember;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.GetFile;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.Progress;
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

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public TelegramService(TelegramProperties telegramProperties, ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
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

    public Message editReplyMarkup(EditMessageReplyMarkup editMessageReplyMarkup) {
        try {
            HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(editMessageReplyMarkup);
            String response = restTemplate.postForObject(getUrl(EditMessageReplyMarkup.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Error editing message reply markup", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), e.getMessage(), e);
        }
    }

    public Message editMessageText(EditMessageText editMessageText) {
        try {
            HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
            String response = restTemplate.postForObject(getUrl(EditMessageText.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(editMessageText.getChatId(), "Error editing message text", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageText.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageText.getChatId(), e.getMessage(), e);
        }
    }

    public Message editMessageCaption(EditMessageCaption editMessageCaption) {
        try {
            HttpEntity<EditMessageCaption> request = new HttpEntity<>(editMessageCaption);
            String response = restTemplate.postForObject(getUrl(EditMessageCaption.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Error editing message caption", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageCaption.getChatId(), e.getMessage(), e);
        }
    }

    public Message editMessageMedia(EditMessageMedia editMessageMedia) {
        try {
            HttpEntity<EditMessageMedia> request = new HttpEntity<>(editMessageMedia);
            String response = restTemplate.postForObject(getUrl(EditMessageMedia.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Error editing message media", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageMedia.getChatId(), e.getMessage(), e);
        }
    }

    public Message sendSticker(SendSticker sendSticker) {
        try {
            HttpEntity<SendSticker> request = new HttpEntity<>(sendSticker);
            String response = restTemplate.postForObject(getUrl(SendSticker.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendSticker.getChatId(), "Error sending sticker", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendSticker.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendSticker.getChatId(), e.getMessage(), e);
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

    public Message sendDocument(SendDocument sendDocument) {
        try {
            HttpEntity<SendDocument> request = new HttpEntity<>(sendDocument);
            String response = restTemplate.postForObject(getUrl(SendDocument.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendDocument.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendDocument.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendDocument.getChatId(), e.getMessage(), e);
        }
    }

    public Message sendVideo(SendVideo sendVideo) {
        try {
            HttpEntity<SendVideo> request = new HttpEntity<>(sendVideo);
            String response = restTemplate.postForObject(getUrl(SendVideo.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendVideo.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendVideo.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendVideo.getChatId(), e.getMessage(), e);
        }
    }

    public Message sendAudio(SendAudio sendAudio) {
        try {
            HttpEntity<SendAudio> request = new HttpEntity<>(sendAudio);
            String response = restTemplate.postForObject(getUrl(SendAudio.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendAudio.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendAudio.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendAudio.getChatId(), e.getMessage(), e);
        }
    }

    public Message sendPhoto(SendPhoto sendPhoto) {
        try {
            HttpEntity<SendPhoto> request = new HttpEntity<>(sendPhoto);
            String response = restTemplate.postForObject(getUrl(SendPhoto.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendPhoto.getChatId(), "Error sending photo", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendPhoto.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendPhoto.getChatId(), e.getMessage(), e);
        }
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

    public MediaType getMediaType(String fileId) {
        try {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("file_id", fileId);
            HttpEntity<ObjectNode> request = new HttpEntity<>(objectNode);
            String response = restTemplate.postForObject(getUrl("head"), request, String.class);
            try {
                ObjectNode result = objectMapper.readValue(response, new TypeReference<>() {
                });
                JsonNode jsonNode = result.get("media_type");

                if (jsonNode.isNull()) {
                    LOGGER.debug("Media type not resolved({})", fileId);

                    return MediaType.DOCUMENT;
                } else {
                    int mediaType = jsonNode.asInt();
                    return MediaType.fromCode(mediaType);
                }
            } catch (IOException e) {
                throw new TelegramApiException(fileId + " Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiException(fileId + " " + e.getMessage(), e);
        }
    }

    public void downloadFileByFileId(String fileId, SmartTempFile outputFile) {
        downloadFileByFileId(fileId, 0, null, outputFile);
    }

    public void downloadFileByFileId(String fileId, long fileSize, Progress progress, SmartTempFile outputFile) {
        downloading.put(fileId, outputFile);
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            LOGGER.debug("Start downloadFileByFileId({})", fileId);

            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            getFile.setFileSize(fileSize);
            getFile.setPath(outputFile.getAbsolutePath());
            getFile.setRemoveParentDirOnCancel(false);
            getFile.setProgress(progress);
            HttpEntity<GetFile> request = new HttpEntity<>(getFile);
            String result = restTemplate.postForObject(getUrl(GetFile.METHOD), request, String.class);
            try {
                ApiResponse<String> apiResponse = objectMapper.readValue(result, new TypeReference<>() {
                });

                if (!apiResponse.getOk()) {
                    throw new DownloadCanceledException();
                }
            } catch (IOException e) {
                throw new TelegramApiException("Unable to deserialize response(" + result + ", " + fileId + ")\n" + e.getMessage(), e);
            }

            stopWatch.stop();
            LOGGER.debug("Finish downloadFileByFileId({}, {}, {})", fileId, MemoryUtils.humanReadableByteCount(outputFile.length()), stopWatch.getTime(TimeUnit.SECONDS));
        } catch (RestClientException e) {
            throw new TelegramApiException(e);
        } finally {
            downloading.remove(fileId);
        }
    }

    public boolean cancelDownloading(String fileId) {
        if (StringUtils.isBlank(fileId)) {
            return false;
        }
        try {
            SmartTempFile tempFile = downloading.get(fileId);
            if (tempFile != null) {
                try {
                    HttpEntity<CancelDownloading> request = new HttpEntity<>(new CancelDownloading(fileId));
                    restTemplate.postForObject(getUrl(CancelDownloading.METHOD), request, Void.class);
                } finally {
                    try {
                        tempFile.smartDelete();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                return true;
            }

            return false;
        } finally {
            downloading.remove(fileId);
        }
    }

    public void cancelDownloads() {
        try {
            for (Map.Entry<String, SmartTempFile> entry : downloading.entrySet()) {
                try {
                    HttpEntity<CancelDownloading> request = new HttpEntity<>(new CancelDownloading(entry.getKey()));
                    restTemplate.postForObject(getUrl(CancelDownloading.METHOD), request, Void.class);
                } finally {
                    try {
                        entry.getValue().smartDelete();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        } finally {
            downloading.clear();
        }
        LOGGER.debug("Downloads canceled");
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        if (!new File(filePath).exists()) {
            downloadFileByFileId(fileId, new SmartTempFile(new File(filePath)));
            LOGGER.debug("File restored({}, {})", fileId, filePath);
        }
    }

    private String getUrl(String method) {
        return telegramProperties.getApi() + method;
    }
}
