package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileService {

    public File createTempFile(String fileName) {
        String tmpDir = System.getProperty("java.io.tmpdir");

        return new File(new File(tmpDir), fileName);
    }
}
