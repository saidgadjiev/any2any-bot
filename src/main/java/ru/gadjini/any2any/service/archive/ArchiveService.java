package ru.gadjini.any2any.service.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.*;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private Set<ArchiveDevice> archiveDevices;

    private FileService fileService;

    private CommonJobExecutor commonJobExecutor;

    private TelegramService telegramService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public ArchiveService(Set<ArchiveDevice> archiveDevices, FileService fileService, CommonJobExecutor commonJobExecutor,
                          TelegramService telegramService, LocalisationService localisationService,
                          @Qualifier("limits") MessageService messageService, UserService userService) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.commonJobExecutor = commonJobExecutor;
        this.telegramService = telegramService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
    }

    public SmartTempFile createArchive(int userId, List<File> files, Format archiveFormat) {
        Locale locale = userService.getLocaleOrDefault(userId);
        SmartTempFile archive = fileService.getTempFile(
                Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), archiveFormat.getExt())
        );
        ArchiveDevice archiveDevice = getCandidate(archiveFormat, locale);
        archiveDevice.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());

        return archive;
    }

    public void createArchive(int userId, List<Any2AnyFile> any2AnyFiles, Format format, Locale locale) {
        commonJobExecutor.addJob(() -> {
            List<SmartTempFile> files = downloadFiles(any2AnyFiles);
            try {
                SmartTempFile archive = fileService.getTempFile(
                        Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), format.getExt())
                );
                try {
                    ArchiveDevice archiveDevice = getCandidate(format, locale);
                    archiveDevice.zip(files.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
                    sendResult(userId, archive.getFile());
                } finally {
                    archive.smartDelete();
                }
            } finally {
                files.forEach(SmartTempFile::smartDelete);
            }
        });
    }

    private void sendResult(int userId, File archive) {
        messageService.sendDocument(new SendFileContext(userId, archive));
    }

    private List<SmartTempFile> downloadFiles(List<Any2AnyFile> any2AnyFiles) {
        List<SmartTempFile> files = new ArrayList<>();

        for (Any2AnyFile any2AnyFile : any2AnyFiles) {
            SmartTempFile file = fileService.createTempFile(any2AnyFile.getFileName());
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file.getFile());
            files.add(file);
        }

        return files;
    }

    private ArchiveDevice getCandidate(Format format, Locale locale) {
        for (ArchiveDevice archiveDevice : archiveDevices) {
            if (archiveDevice.accept(format)) {
                return archiveDevice;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_TYPE_UNSUPPORTED, locale));
    }
}
