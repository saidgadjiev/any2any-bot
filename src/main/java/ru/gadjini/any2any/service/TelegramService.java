package ru.gadjini.any2any.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.object.GetFile;

import java.io.File;

@Service
//TODO: работа с mt proto должно из message service перейти сюда
public class TelegramService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

    private static final String API = "http://localhost:5000/";

    private TempFileService fileService;

    private RestTemplate restTemplate;

    @Autowired
    public TelegramService(TempFileService fileService) {
        this.fileService = fileService;
        this.restTemplate = new RestTemplate();
    }

    public void downloadFileByFileId(String fileId, File outputFile) {
        HttpEntity<GetFile> request = new HttpEntity<>(new GetFile(fileId, outputFile.getAbsolutePath()));
        ResponseEntity responseEntity = restTemplate.postForEntity(API + "downloadfile", request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
            throw new TelegramRequestException(responseEntity.getStatusCodeValue());
        }
    }

    public SmartTempFile downloadFileByFileId(String fileId, String ext) {
        SmartTempFile smartTempFile = fileService.createTempFile0(fileId, ext);
        try {
            downloadFileByFileId(fileId, smartTempFile.getFile());
        } catch (Exception ex) {
            smartTempFile.smartDelete();
            throw ex;
        }

        return smartTempFile;
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        if (!new File(filePath).exists()) {
            downloadFileByFileId(fileId, new File(filePath));
            LOGGER.debug("Temp file " + filePath + " file id " + fileId + " restored");
        }
    }
}
