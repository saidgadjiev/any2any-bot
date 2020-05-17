package ru.gadjini.any2any.service.unzip;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.service.converter.api.Format;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Conditional({LinuxMacCondition.class})
public class P7ZipService extends BaseZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(P7ZipService.class);

    protected P7ZipService() {
        super(Set.of(Format.ZIP, Format.RAR));
    }

    @Override
    public void unzip(int userId, String in, String out) {
        try {
            Process process = Runtime.getRuntime().exec(buildUnzipCommand(in, out));
            try {
                boolean result = process.waitFor(10, TimeUnit.SECONDS);
                if (!result) {
                    throw new RuntimeException("Timed out");
                }
                if (process.exitValue() != 0) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    LOGGER.error("Exit code " + process.exitValue() + " out: " + error + " in " + in);
                }
            } finally {
                process.destroy();
            }
        } catch (Exception ex) {
            throw new UnzipException(ex);
        }
    }

    private String buildUnzipCommand(String in, String out) {
        return "7z x " + in + " -y -o" + out;
    }
}
