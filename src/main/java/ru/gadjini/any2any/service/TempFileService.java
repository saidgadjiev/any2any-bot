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

    public SmartTempFile createTempFile0(String prefix, String ext) {
        try {
            File file = new File(tempDir, generateName(prefix, ext));
            Files.createFile(file.toPath());

            LOGGER.debug("Temp file({})", file.getName());
            return new SmartTempFile(file, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile getTempFileWithExt(String ext) {
        File tmpFile = new File(tempDir, generateName(null, ext));

        LOGGER.debug("Temp dir({})", tmpFile.getName());
        return new SmartTempFile(tmpFile, false);
    }

    public SmartTempFile createTempFile(String fileName) {
        try {
            File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
            Files.createDirectory(tmpDir.toPath());
            File file = new File(tmpDir, fileName);
            Files.createFile(file.toPath());

            LOGGER.debug("Temp file({})", file.getAbsolutePath());
            return new SmartTempFile(file, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile createTempDir() {
        try {
            File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
            Files.createDirectory(tmpDir.toPath());

            LOGGER.debug("Temp dir({})", tmpDir.getAbsolutePath());
            return new SmartTempFile(tmpDir, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateName(String prefix, String ext) {
        prefix = StringUtils.defaultIfBlank(prefix, "");
        ext = StringUtils.defaultIfBlank(ext, "tmp");
        long n = RANDOM.nextLong();

        return prefix + Long.toUnsignedString(n) + "." + ext;
    }

    private String generateUniquePart() {
        long n = RANDOM.nextLong();

        return Long.toUnsignedString(n);
    }
}
