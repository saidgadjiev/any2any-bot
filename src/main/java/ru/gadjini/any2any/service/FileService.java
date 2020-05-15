package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class FileService {

    public File createTempFile0(String prefix, String ext) {
        try {
            return File.createTempFile(prefix, "." + ext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File createTempFile(String fileName) {
        String tmpDir = System.getProperty("java.io.tmpdir");

        return new File(new File(tmpDir), fileName);
    }

    public File createTempDir(String name) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpDir, name);

        file.mkdirs();

        return file;
    }
}
