package ru.gadjini.any2any.service.archive;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.ArrayList;
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
        new ProcessExecutor().execute(buildCommand(files, out), 2 * 60);
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
