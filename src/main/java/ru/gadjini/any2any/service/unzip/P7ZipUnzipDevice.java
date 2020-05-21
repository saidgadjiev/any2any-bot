package ru.gadjini.any2any.service.unzip;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Set;

@Component
@Conditional({LinuxMacCondition.class})
public class P7ZipUnzipDevice extends BaseUnzipDevice {

    protected P7ZipUnzipDevice() {
        super(Set.of(Format.ZIP, Format.RAR));
    }

    @Override
    public void unzip(int userId, String in, String out) {
        new ProcessExecutor().execute(buildUnzipCommand(in, out), 10);
    }

    private String buildUnzipCommand(String in, String out) {
        return "7z x " + in + " -y -o" + out;
    }
}
