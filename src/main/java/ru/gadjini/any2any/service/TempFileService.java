package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;

@Service
public class TempFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFileService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${temp.dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

    @PostConstruct
    public void init() {
        LOGGER.debug("Temp dir({})", tempDir);
    }

    public String getTempDir() {
        return tempDir;
    }

    public SmartTempFile getTempFile(String prefix, String ext) {
        File file = new File(tempDir, generateName(prefix, ext));

        LOGGER.debug("Get({})", file.getAbsolutePath());
        return new SmartTempFile(file);
    }

    public SmartTempFile createTempFile(String prefix, String ext) {
        try {
            File file = new File(tempDir, generateName(prefix, ext));
            Files.createFile(file.toPath());

            LOGGER.debug("Create({})", file.getAbsolutePath());
            return new SmartTempFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateName(String prefix, String ext) {
        prefix = StringUtils.defaultIfBlank(prefix, "");
        ext = StringUtils.defaultIfBlank(ext, "tmp");
        long n = RANDOM.nextLong();

        return prefix + Long.toUnsignedString(n) + "." + ext;
    }
}
