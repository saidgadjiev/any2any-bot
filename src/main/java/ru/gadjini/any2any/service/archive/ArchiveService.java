package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.*;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private Set<ZipProgram> zipPrograms;

    private FileService fileService;

    private CommonJobExecutor commonJobExecutor;

    private TelegramService telegramService;

    private LocalisationService localisationService;

    private FormatService formatService;

    private UserService userService;

    private MessageService messageService;

    @Autowired
    public ArchiveService(Set<ZipProgram> zipPrograms, FileService fileService, CommonJobExecutor commonJobExecutor,
                          TelegramService telegramService, LocalisationService localisationService,
                          FormatService formatService, UserService userService, @Qualifier("limits") MessageService messageService) {
        this.zipPrograms = zipPrograms;
        this.fileService = fileService;
        this.commonJobExecutor = commonJobExecutor;
        this.telegramService = telegramService;
        this.localisationService = localisationService;
        this.formatService = formatService;
        this.userService = userService;
        this.messageService = messageService;
    }

    public void createArchive(int userId, List<Any2AnyFile> any2AnyFiles, Format format, Locale locale) {
        commonJobExecutor.addJob(() -> {
            List<File> files = downloadFiles(any2AnyFiles);
            try {
                File archive = fileService.createTempFile(
                        Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), format.getExt())
                );
                try {
                    ZipProgram zipProgram = getCandidate(format, locale);
                    zipProgram.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
                    sendResult(userId, archive);
                } finally {
                    FileUtils.deleteQuietly(archive);
                }
            } finally {
                files.forEach(file -> FileUtils.deleteQuietly(file.getParentFile()));
            }
        });
    }

    private void sendResult(int userId, File archive) {
        messageService.sendDocument(new SendFileContext(userId, archive));
    }

    private List<File> downloadFiles(List<Any2AnyFile> any2AnyFiles) {
        List<File> files = new ArrayList<>();

        for (Any2AnyFile any2AnyFile : any2AnyFiles) {
            File file = fileService.createTempFile(any2AnyFile.getFileName());
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file);
            files.add(file);
        }

        return files;
    }

    private ZipProgram getCandidate(Format format, Locale locale) {
        for (ZipProgram zipProgram : zipPrograms) {
            if (zipProgram.accept(format)) {
                return zipProgram;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_TYPE_UNSUPPORTED, locale));
    }
}
