package ru.gadjini.any2any.service.cleaner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.unzip.UnzipState;
import ru.gadjini.any2any.utils.FileUtils2;

import java.io.File;
import java.util.Collection;

@Component
public class G2Algorithm implements GarbageAlgorithm {

    private CommandStateService commandStateService;

    @Autowired
    public G2Algorithm(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Override
    public boolean accept(File file) {
        String fileName = file.getName();
        if (StringUtils.isBlank(fileName)) {
            return false;
        }

        return Constants.FILE_TAGS.stream().noneMatch(fileName::contains);
    }

    @Override
    public boolean isGarbage(File file) {
        if (!FileUtils2.isExpired(file, 1)) {
            return false;
        }
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
}
