package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ImageEditorService {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");

    private ImageDevice imageDevice;

    private CommonJobExecutor commonJobExecutor;

    private TelegramService telegramService;

    private FileService fileService;

    private Map<Long, EditorState> stateMap = new HashMap<>();

    private MessageService messageService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ImageEditorService(ImageDevice imageDevice, CommonJobExecutor commonJobExecutor,
                              TelegramService telegramService, FileService fileService,
                              @Qualifier("limits") MessageService messageService,
                              LocalisationService localisationService, InlineKeyboardService inlineKeyboardService) {
        this.imageDevice = imageDevice;
        this.commonJobExecutor = commonJobExecutor;
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    public void changeScreen(long chatId, EditorState.Screen screen) {
        EditorState state = stateMap.get(chatId);
        state.setScreen(screen);
        changeScreenKeyboard(chatId, state.getMessageId(), screen, new Locale(state.getLanguage()));
    }

    public void transparentMode(long chatId, EditorState.Mode mode) {
        EditorState state = stateMap.get(chatId);
        state.setMode(mode);
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
                        .replyKeyboard(inlineKeyboardService.getEditImageTransparentKeyboard(locale)));
                state.setMessageId(messageId);
            } finally {
                file.smartDelete();
            }
        });
    }

    public void transparentColor(long chatId, String colorText) {
        commonJobExecutor.addJob(() -> {
            EditorState editorState = stateMap.get(chatId);
            SmartTempFile tempFile = fileService.getTempFile(editorState.getFileName());

            if (editorState.getMode() == EditorState.Mode.NEGATIVE) {
                String[] transparentColors = getNegativeTransparentColors(colorText);
                imageDevice.negativeTransparent(editorState.getEditFilePath(), tempFile.getAbsolutePath(), transparentColors);
            } else {
                imageDevice.negativeTransparent(editorState.getEditFilePath(), tempFile.getAbsolutePath(), getPositiveTransparentColor(colorText));
            }
            try {
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getEditFilePath()), true);
                prevFile.smartDelete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            editorState.setEditFilePath(tempFile.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), tempFile.getFile())
                    .replyKeyboard(getScreenKeyboard(editorState.getScreen(), locale)));
        });
    }

    private EditorState createState(long chatId, String filePath, String fileName) {
        EditorState editorState = new EditorState();
        editorState.setImage(filePath);
        editorState.setFileName(fileName);
        stateMap.put(chatId, editorState);

        return editorState;
    }

    private void changeScreenKeyboard(long chatId, int messageId, EditorState.Screen screen, Locale locale) {
        messageService.editReplyKeyboard(chatId, messageId, getScreenKeyboard(screen, locale));
    }

    private InlineKeyboardMarkup getScreenKeyboard(EditorState.Screen screen, Locale locale) {
        if (screen == EditorState.Screen.EDIT) {
            return inlineKeyboardService.getEditImageTransparentKeyboard(locale);
        }
        return inlineKeyboardService.getColorsKeyboard(locale);
    }

    private String getPositiveTransparentColor(String colorText) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return col.name().toLowerCase();
            }
        }
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;
        if (HEX.matcher(colorText).matches()) {
            return colorText;
        }

        throw new IllegalStateException();
    }

    private String[] getNegativeTransparentColors(String colorText) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return col.transparentColors();
            }
        }
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;
        if (HEX.matcher(colorText).matches()) {
            return new String[]{colorText};
        }

        throw new IllegalStateException();
    }
}
