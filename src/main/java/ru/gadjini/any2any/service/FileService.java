package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService {

    public SmartTempFile createTempFile0(String prefix, String ext) {
        try {
            return new SmartTempFile(File.createTempFile(prefix, "." + ext), false);
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
            Path tmpdir = Files.createTempDirectory("tmpdir");

            File file = new File(tmpdir.toFile(), fileName);
            file.createNewFile();

            return new SmartTempFile(file, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile createTempDir(String name) {
        try {
            Path tmpdir = Files.createTempDirectory("tmpdir");
            File file = new File(tmpdir.toFile(), name);

            file.mkdirs();

            return new SmartTempFile(file, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
