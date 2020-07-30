package ru.gadjini.any2any.service.thumb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;

@Service
public class ThumbService {

    public static final String TAG = "thumb";

    private TelegramService telegramService;

    private TempFileService tempFileService;

    private FormatService formatService;

    private ImageConvertDevice convertDevice;

    @Autowired
    public ThumbService(TelegramService telegramService, TempFileService tempFileService,
                        FormatService formatService, ImageConvertDevice convertDevice) {
        this.telegramService = telegramService;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.convertDevice = convertDevice;
    }

    public SmartTempFile convertToThumb(long chatId, String fileId, String fileName, String mimeType) {
        String ext = formatService.getExt(fileName, mimeType);
        SmartTempFile thumb = tempFileService.createTempFile(chatId, fileId, TAG, ext);
        try {
            telegramService.downloadFileByFileId(fileId, thumb);
            SmartTempFile out = tempFileService.createTempFile(chatId, fileId, TAG, Format.JPG.getExt());
            try {
                convertDevice.convertToThumb(thumb.getAbsolutePath(), out.getAbsolutePath());

                return out;
            } catch (Exception e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            thumb.smartDelete();
        }
    }
}
