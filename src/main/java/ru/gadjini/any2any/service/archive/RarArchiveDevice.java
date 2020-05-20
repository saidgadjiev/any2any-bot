package ru.gadjini.any2any.service.archive;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
@Conditional(LinuxMacCondition.class)
public class RarArchiveDevice extends BaseArchiveDevice {

    protected RarArchiveDevice() {
        super(Set.of(Format.RAR));
    }

    @Override
    public void zip(List<String> files, String out) {
        new ProcessExecutor().execute(buildCommand(files, out));
    }

    private String buildCommand(List<String> files, String out) {
        StringBuilder command = new StringBuilder();
        command.append("rar a ").append(out).append(" ");
        for (Iterator<String> fileIterator = files.iterator(); fileIterator.hasNext();) {
            String file = fileIterator.next();
            command.append(file);
            if (fileIterator.hasNext()) {
                command.append(" ");
            }
        }

        return command.toString();
    }
}
