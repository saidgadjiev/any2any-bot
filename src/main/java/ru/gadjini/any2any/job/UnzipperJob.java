package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.unzip.UnzipResult;
import ru.gadjini.any2any.service.unzip.Unzipper;

import java.io.File;
import java.util.List;

@Component
public class UnzipperJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipperJob.class);

    private ThreadPoolTaskExecutor taskExecutor;

    private MessageService messageService;

    @Autowired
    public UnzipperJob(ThreadPoolTaskExecutor taskExecutor, MessageService messageService) {
        this.taskExecutor = taskExecutor;
        this.messageService = messageService;
    }

    public void addJob(UnzipJob unzipJob) {
        LOGGER.debug("New unzip job {}", unzipJob.toString());
        taskExecutor.execute(() -> {
            try (UnzipResult unzip = unzipJob.unzipper.unzip(unzipJob.fileId, unzipJob.format)) {
                sendFiles(unzipJob.userId, unzip.getFiles());
            }
        });
    }

    private void sendFiles(int userId, List<File> files) {
        for (File file : files) {
            messageService.sendDocument(new SendFileContext(userId, file));
        }
    }

    public static class UnzipJob {

        private Unzipper unzipper;

        private String fileId;

        private int userId;

        private Format format;

        public UnzipJob(Unzipper unzipper, String fileId, int userId, Format format) {
            this.unzipper = unzipper;
            this.fileId = fileId;
            this.userId = userId;
            this.format = format;
        }

        @Override
        public String toString() {
            return "UnzipJob{" +
                    "fileId='" + fileId + '\'' +
                    ", userId=" + userId +
                    ", format=" + format +
                    '}';
        }
    }
}
