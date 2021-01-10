package ru.gadjini.any2any.service.archive;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.WindowsCondition;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Set;

@Component
@Conditional(WindowsCondition.class)
public class WindowsZipRarArchiveDevice extends BaseArchiveDevice {

    protected WindowsZipRarArchiveDevice() {
        super(Set.of(Format.ZIP, Format.RAR));
    }

    @Override
    public void zip(List<String> files, String out) {
        throw new NotImplementedException("No implementation for windows");
    }

    @Override
    public void delete(String archive, String fileHeader) {
        throw new NotImplementedException("No implementation for windows");
    }
}
