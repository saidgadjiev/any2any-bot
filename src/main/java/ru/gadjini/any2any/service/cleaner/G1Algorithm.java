package ru.gadjini.any2any.service.cleaner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.StateFather;
import ru.gadjini.any2any.service.image.editor.filter.FilterState;
import ru.gadjini.any2any.service.image.editor.transparency.ColorState;
import ru.gadjini.any2any.service.unzip.UnzipService;
import ru.gadjini.any2any.service.unzip.UnzipState;

import java.io.File;
import java.util.Collection;
import java.util.Set;

@Component
public class G1Algorithm implements GarbageAlgorithm {

    private Set<String> garbageTags = Set.of(
            FilterState.TAG,
            UnzipService.UnzipTask.TAG,
            ColorState.TAG,
            StateFather.TAG
    );

    private CommandStateService commandStateService;

    @Autowired
    public G1Algorithm(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
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
        if (garbageTags.stream().noneMatch(s -> file.getName().startsWith(s))) {
            return false;
        }
        Collection<Object> allStates = commandStateService.getAllStates();
        boolean garbage = true;

        for (Object state : allStates) {
            if (state instanceof UnzipState && ((UnzipState) state).getArchivePath().equals(file.getAbsolutePath())) {
                garbage = false;
                break;
            }
            if (state instanceof EditorState && (((EditorState) state).getCurrentFilePath().equals(file.getAbsolutePath()) ||
                    ((EditorState) state).getPrevFilePath().equals(file.getAbsolutePath()))) {
                garbage = false;
                break;
            }
        }

        return garbage;
    }
}
