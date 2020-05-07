package ru.gadjini.any2any.service;

import com.aspose.words.Run;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class FileService {

    public File createTempFile(String prefix, String ext) {
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
}
