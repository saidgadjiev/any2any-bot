package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

@Service
public class TempFileService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("temp.dir")
    private String tempDir;

    @Autowired
    public TempFileService() {
        if (StringUtils.isBlank(tempDir)) {
            tempDir = System.getProperty("java.io.tmpdir");
        }
    }

    public SmartTempFile createTempFile0(String prefix, String ext) {
        try {
            File file = new File(tempDir, generateName(prefix, ext));
            file.createNewFile();

            return new SmartTempFile(file, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile getTempFile(String fileName) {
        try {
            Path tmpdir = Files.createTempDirectory("tmpdir");

            return new SmartTempFile(new File(tmpdir.toFile(), fileName), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile createTempFile(String fileName) {
        try {
            File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
            File file = new File(tmpDir, fileName);
            file.createNewFile();

            return new SmartTempFile(file, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile createTempDir(String name) {
        File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
        File file = new File(tmpDir, name);
        file.mkdirs();

        return new SmartTempFile(file, true);
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
