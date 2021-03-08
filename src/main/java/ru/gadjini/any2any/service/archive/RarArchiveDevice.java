package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Conditional(LinuxMacCondition.class)
public class RarArchiveDevice extends BaseArchiveDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public RarArchiveDevice(ProcessExecutor processExecutor) {
        super(Set.of(Format.RAR));
        this.processExecutor = processExecutor;
    }

    @Override
    public void zip(List<String> files, String out) {
        try {
            processExecutor.execute(buildCommand(files, out));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public void delete(String archive, String fileHeader) {
        try {
            processExecutor.execute(buildDeleteCommand(archive, fileHeader));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String rename(String archive, String fileHeader, String newFileName) {
        String newHeader = buildNewHeader(fileHeader, newFileName);
        try {
            processExecutor.execute(buildRenameCommand(archive, fileHeader, newHeader));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return newHeader;
    }

    private String[] buildRenameCommand(String archive, String fileHeader, String newFileHeader) {
        return new String[] {"rar", "rn", archive, fileHeader, "--", newFileHeader.replaceFirst("@", "")};
    }

    private String buildNewHeader(String fileHeader, String newFileName) {
        String path = FilenameUtils.getFullPath(fileHeader);

        return path + newFileName;
    }

    private String[] buildDeleteCommand(String archive, String fileHeader) {
        return new String[]{
                "rar", "d", archive, fileHeader
        };
    }

    private String[] buildCommand(List<String> files, String out) {
        List<String> command = new ArrayList<>();
        command.add("rar");
        command.add("a");
        command.add("-ep");
        command.add(out);
        command.addAll(files);

        return command.toArray(new String[0]);
    }
}
