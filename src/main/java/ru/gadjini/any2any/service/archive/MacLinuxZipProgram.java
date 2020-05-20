package ru.gadjini.any2any.service.archive;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;
import java.util.Set;

@Component
@Conditional(LinuxMacCondition.class)
public class MacLinuxZipProgram extends BaseZipProgram {

    protected MacLinuxZipProgram() {
        super(Set.of(Format.ZIP));
    }

    @Override
    public void zip(List<String> files, String out) {
    }

    private String buildCommand(List<String> files, String out) {
        return "";
    }
}
