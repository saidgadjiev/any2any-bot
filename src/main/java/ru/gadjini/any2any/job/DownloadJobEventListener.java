package ru.gadjini.any2any.job;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveDevice;
import ru.gadjini.any2any.service.queue.ArchiveQueueService;
import ru.gadjini.telegram.smart.bot.commons.common.TgConstants;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.DownloadCompleted;
import ru.gadjini.telegram.smart.bot.commons.utils.SmartFileUtils;

import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

@Component
public class DownloadJobEventListener {

    private static final String TAG = "archive";

    private Gson gson;

    private Set<ArchiveDevice> archiveDevices;

    private FileDownloadService fileDownloadService;

    private TempFileService tempFileService;

    private ArchiveQueueService archiveQueueService;

    @Autowired
    public DownloadJobEventListener(Gson gson, Set<ArchiveDevice> archiveDevices, FileDownloadService fileDownloadService,
                                    TempFileService tempFileService, ArchiveQueueService archiveQueueService) {
        this.gson = gson;
        this.archiveDevices = archiveDevices;
        this.fileDownloadService = fileDownloadService;
        this.tempFileService = tempFileService;
        this.archiveQueueService = archiveQueueService;
    }

    @EventListener
    public void downloadCompleted(DownloadCompleted downloadCompleted) {
        ArchiveQueueItem queueItem = archiveQueueService.getArchiveTypeAndArchivePath(downloadCompleted.getDownloadQueueItem().getProducerId());
        if (StringUtils.isBlank(queueItem.getArchiveFilePath())) {
            SmartTempFile archive = tempFileService.getTempFile(downloadCompleted.getDownloadQueueItem().getUserId(), TAG, queueItem.getType().getExt());
            queueItem.setArchiveFilePath(archive.getAbsolutePath());
            archiveQueueService.setArchiveFilePath(queueItem.getId(), archive.getAbsolutePath());
        }
        ArchiveDevice archiveDevice = getCandidate(queueItem.getType());
        try {
            archiveDevice.zip(List.of(downloadCompleted.getDownloadQueueItem().getFilePath()), queueItem.getArchiveFilePath());
            archiveDevice.rename(queueItem.getArchiveFilePath(), downloadCompleted.getDownloadQueueItem().getFilePath(),
                    downloadCompleted.getDownloadQueueItem().getFile().getFileName());

            if (SmartFileUtils.getLength(queueItem.getArchiveFilePath()) > TgConstants.LARGE_FILE_SIZE) {
                archiveQueueService.setArchiveIsReady(downloadCompleted.getDownloadQueueItem().getProducerId());
                return;
            }
        } finally {
            new SmartTempFile(new File(downloadCompleted.getDownloadQueueItem().getFilePath())).smartDelete();
        }

        if (downloadCompleted.getDownloadQueueItem().getExtra() != null) {
            DownloadExtra downloadExtra = gson.fromJson((JsonElement) downloadCompleted.getDownloadQueueItem().getExtra(), DownloadExtra.class);
            ListIterator<TgFile> listIterator = downloadExtra.getFiles().listIterator(downloadExtra.getCurrentFileIndex() + 1);

            if (listIterator.hasNext()) {
                TgFile file = listIterator.next();
                fileDownloadService.createDownload(file, downloadCompleted.getDownloadQueueItem().getProducerId(),
                        downloadCompleted.getDownloadQueueItem().getUserId(), new DownloadExtra(downloadExtra.getFiles(), downloadExtra.getCurrentFileIndex() + 1));
            } else {
                archiveQueueService.setArchiveIsReady(downloadCompleted.getDownloadQueueItem().getProducerId());
            }
        } else {
            archiveQueueService.setArchiveIsReady(downloadCompleted.getDownloadQueueItem().getProducerId());
        }
    }

    private ArchiveDevice getCandidate(Format format) {
        for (ArchiveDevice archiveDevice : archiveDevices) {
            if (archiveDevice.accept(format)) {
                return archiveDevice;
            }
        }

        throw new IllegalArgumentException("No candidate for " + format);
    }
}
