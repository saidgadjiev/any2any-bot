package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

@Service
public class TempFileService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${temp.dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

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
        File tmpdir = new File(tempDir, "tmpdir" + generateUniquePart());

        return new SmartTempFile(new File(tmpdir, fileName), true);
    }

    public SmartTempFile createTempFile(String fileName) {
        try {
            File tmpDir = new File(tempDir, "tmpdir" + generateUniquePart());
            tmpDir.mkdirs();
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
