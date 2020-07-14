package ru.gadjini.any2any.service;

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
            file.createNewFile();

            LOGGER.debug("Temp file({})", file.getAbsolutePath());
            return new SmartTempFile(file, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile getTempFile(String fileName) {
        try {
            File tmpdir = new File(tempDir, "tmpdir" + generateUniquePart());

            Files.createDirectory(tmpdir.toPath());

            LOGGER.debug("Temp dir({})", tmpdir.getAbsolutePath());
            return new SmartTempFile(new File(tmpdir, fileName), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public SmartTempFile createTempDir(String name) {
        try {
            File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
            File file = new File(tmpDir, name);
            Files.createDirectories(file.toPath());

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
        long n = RANDOM.nextLong();

        return prefix + Long.toUnsignedString(n) + "." + ext;
    }

    private String generateUniquePart() {
        long n = RANDOM.nextLong();

        return Long.toUnsignedString(n);
    }
}
