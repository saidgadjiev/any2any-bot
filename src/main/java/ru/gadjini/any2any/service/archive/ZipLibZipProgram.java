package ru.gadjini.any2any.service.archive;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.WindowsCondition;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;
import java.util.Set;

@Component
@Conditional(WindowsCondition.class)
public class ZipLibZipProgram extends BaseZipProgram {

    protected ZipLibZipProgram() {
        super(Set.of(Format.ZIP));
    }

    @Override
    public void zip(List<String> files, String out) {

    }
}
