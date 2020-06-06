package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ImageEditorService {

    private ImageDevice imageDevice;

    private CommonJobExecutor commonJobExecutor;

    private TelegramService telegramService;

    private FileService fileService;

    private Map<Long, EditorState> stateMap = new HashMap<>();

    private MessageService messageService;

    private LocalisationService localisationService;

    @Autowired
    public ImageEditorService(ImageDevice imageDevice, CommonJobExecutor commonJobExecutor,
                              TelegramService telegramService, FileService fileService,
                              @Qualifier("limits") MessageService messageService, LocalisationService localisationService) {
        this.imageDevice = imageDevice;
        this.commonJobExecutor = commonJobExecutor;
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.messageService = messageService;
        this.localisationService = localisationService;
    }

    public void editFile(long chatId, Any2AnyFile any2AnyFile, Locale locale) {
        commonJobExecutor.addJob(() -> {
            SmartTempFile file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(any2AnyFile.getFileName(), any2AnyFile.getFormat().getExt()));
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file.getFile());
            try {
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(file.getName(), Format.PNG.getExt()));
                imageDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());
                EditorState state = createState(chatId, result.getAbsolutePath(), result.getName());
                state.setLanguage(locale.getLanguage());
                int messageId = messageService.sendDocument(new SendFileContext(chatId, result.getFile())
                        .caption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_WELCOME, locale)));
                state.setMessageId(messageId);
            } finally {
                file.smartDelete();
            }
        });
    }

    public void transparentColor(long chatId, String color) {
        commonJobExecutor.addJob(() -> {
            EditorState editorState = stateMap.get(chatId);
            SmartTempFile tempFile = fileService.getTempFile(editorState.getFileName());

            imageDevice.transparent(editorState.getEditFilePath(), tempFile.getAbsolutePath(), color, editorState.getMode() == EditorState.Mode.REMOVE);
            try {
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getEditFilePath()), true);
                prevFile.smartDelete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            editorState.setEditFilePath(tempFile.getAbsolutePath());
            messageService.editMessageMedia(chatId, editorState.getMessageId(), tempFile.getFile());
        });
    }

    private EditorState createState(long chatId, String filePath, String fileName) {
        EditorState editorState = new EditorState();
        editorState.setImage(filePath);
        editorState.setFileName(fileName);
        stateMap.put(chatId, editorState);

        return editorState;
    }
}
