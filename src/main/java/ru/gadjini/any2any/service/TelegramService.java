package ru.gadjini.any2any.service;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.model.bot.api.object.GetFile;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
//TODO: работа с mt proto должно из message service перейти сюда
public class TelegramService {

    private static final SmartLogger LOGGER = new SmartLogger(TelegramService.class);

    private static final String API = "http://localhost:5000/";

    private TempFileService fileService;

    private RestTemplate restTemplate;

    @Autowired
    public TelegramService(TempFileService fileService) {
        this.fileService = fileService;
        this.restTemplate = new RestTemplate();
    }

    public void downloadFileByFileId(String fileId, File outputFile) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOGGER.debug("Start downloadFileByFileId", fileId);

        HttpEntity<GetFile> request = new HttpEntity<>(new GetFile(fileId, outputFile.getAbsolutePath()));
        ResponseEntity responseEntity = restTemplate.postForEntity(API + "downloadfile", request, Void.class);

        LOGGER.debug("Finish downloadFileByFileId", fileId, stopWatch.getTime(TimeUnit.SECONDS));
        stopWatch.stop();

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("downloadFileByFileId", responseEntity.getStatusCode().value());
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
            LOGGER.debug("File restored", fileId, filePath);
        }
    }
}
