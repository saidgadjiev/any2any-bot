package ru.gadjini.any2any.service.cleaner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.StateFather;
import ru.gadjini.any2any.service.image.editor.filter.FilterState;
import ru.gadjini.any2any.service.image.editor.transparency.ColorState;
import ru.gadjini.any2any.service.image.resize.ResizeState;
import ru.gadjini.any2any.service.ocr.OcrService;
import ru.gadjini.any2any.service.queue.rename.RenameQueueService;
import ru.gadjini.any2any.service.unzip.UnzipService;
import ru.gadjini.any2any.service.unzip.UnzipState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;

@Component
public class G1Algorithm implements GarbageAlgorithm {

    private Set<String> garbageTags = Set.of(
            FilterState.TAG,
            UnzipService.UnzipTask.TAG,
            ColorState.TAG,
            StateFather.TAG,
            ResizeState.TAG,
            RenameService.RenameTask.TAG,
            OcrService.TAG
    );

    private Set<String> imagEditorGarbageTags = Set.of(
            FilterState.TAG,
            ColorState.TAG,
            StateFather.TAG,
            ResizeState.TAG
    );

    private CommandStateService commandStateService;

    private RenameQueueService renameQueueService;

    @Autowired
    public G1Algorithm(CommandStateService commandStateService, RenameQueueService renameQueueService) {
        this.commandStateService = commandStateService;
        this.renameQueueService = renameQueueService;
    }

    @Override
    public boolean accept(File file) {
        String fileName = file.getName();
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        return Constants.FILE_TAGS.stream().anyMatch(fileName::startsWith);
    }

    @Override
    public boolean isGarbage(File file) {
        if (!isExpired(file)) {
            return false;
        }
        if (garbageTags.stream().noneMatch(s -> file.getName().startsWith(s))) {
            return false;
        }
        Collection<Object> allStates = commandStateService.getAllStates();
        boolean garbage = true;

        for (Object state : allStates) {
            if (file.getName().startsWith(UnzipService.UnzipTask.TAG)) {
                if (state instanceof UnzipState && ((UnzipState) state).getArchivePath().equals(file.getAbsolutePath())) {
                    garbage = false;
                    break;
                }
            } else if (file.getName().startsWith(RenameService.RenameTask.TAG)) {
                if (state instanceof RenameState && renameQueueService.exists(((RenameState) state).getReplyMessageId())) {
                    garbage = false;
                    break;
                }
            } else if (imagEditorGarbageTags.stream().anyMatch(f -> file.getName().startsWith(f))
                    && state instanceof EditorState && (((EditorState) state).getCurrentFilePath().equals(file.getAbsolutePath()) ||
                    ((EditorState) state).getPrevFilePath().equals(file.getAbsolutePath()))) {
                garbage = false;
                break;
            }
        }

        return garbage;
    }

    private boolean isExpired(File file) {
        try {
            FileTime creationTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");

            LocalDateTime creationDateTime = creationTime.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

            long between = ChronoUnit.DAYS.between(creationDateTime, LocalDateTime.now());
            if (between > 0) {
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
