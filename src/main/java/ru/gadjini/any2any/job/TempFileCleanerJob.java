package ru.gadjini.any2any.job;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.filter.FilterState;
import ru.gadjini.any2any.service.unzip.UnzipService;
import ru.gadjini.any2any.service.unzip.UnzipState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

@Component
public class TempFileCleanerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFileCleanerJob.class);

    private final Set<Function<File, Function<File, Boolean>>> ALGORITHMS = Set.of(
            file -> {
                String fileName = file.getName();
                if (isOldTagged(fileName)) {
                    return new Function<>() {
                        @Override
                        public Boolean apply(File file) {
                            Collection<Object> allStates = commandStateService.getAllStates();
                            boolean garbage = true;

                            for (Object state : allStates) {
                                if (state instanceof UnzipState && ((UnzipState) state).getArchivePath().equals(file.getAbsolutePath())) {
                                    garbage = false;
                                    break;
                                } else if (state instanceof EditorState && (((EditorState) state).getCurrentFilePath().equals(file.getAbsolutePath()) ||
                                        ((EditorState) state).getPrevFilePath().equals(file.getAbsolutePath()))) {
                                    garbage = false;
                                    break;
                                }
                            }

                            return garbage;
                        }
                    };
                }

                return null;
            }
    );

    private TempFileService tempFileService;

    private CommandStateService commandStateService;

    @Autowired
    public TempFileCleanerJob(TempFileService tempFileService, CommandStateService commandStateService) {
        this.tempFileService = tempFileService;
        this.commandStateService = commandStateService;
    }

    @Scheduled
    public void clean() throws IOException {
        Files.list(Path.of(tempFileService.getTempDir()))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(this::deleteIfGarbage);
    }

    private void deleteIfGarbage(File file) {
        Function<File, Boolean> algorithm = getAlgorithm(file);

        if (algorithm != null) {
            if (algorithm.apply(file)) {
                FileUtils.deleteQuietly(file);
            }
        } else {
            LOGGER.debug("Algorithm not found({})", file.getAbsolutePath());
        }
    }

    private Function<File, Boolean> getAlgorithm(File file) {
        for (Function<File, Function<File, Boolean>> function: ALGORITHMS) {
            Function<File, Boolean> apply = function.apply(file);

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    private boolean isOldTagged(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        return fileName.startsWith(UnzipService.UnzipTask.TAG)
                || fileName.startsWith(FilterState.TAG)
                || fileName.startsWith(ArchiveService.ArchiveTask.TAG);
    }
}
