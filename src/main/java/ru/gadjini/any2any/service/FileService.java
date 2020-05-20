package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService {

    public File createTempFile0(String prefix, String ext) {
        try {
            return File.createTempFile(prefix, "." + ext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getTempFile(String fileName) {
        try {
            Path tmpdir = Files.createTempDirectory("tmpdir");

            return new File(tmpdir.toFile(), fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File createTempFile(String fileName) {
        try {
            Path tmpdir = Files.createTempDirectory("tmpdir");

            File file = new File(tmpdir.toFile(), fileName);
            file.createNewFile();

            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File createTempDir(String name) {
        try {
            Path tmpdir = Files.createTempDirectory("tmpdir");
            File file = new File(tmpdir.toFile(), name);

            file.mkdirs();

            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
