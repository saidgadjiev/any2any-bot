package ru.gadjini.any2any.service.unzip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

public class UnzipResult implements AutoCloseable {

    private List<File> files;

    private File rootDir;

    public UnzipResult(List<File> files, File rootDir) {
        this.files = files;
        this.rootDir = rootDir;
    }

    public List<File> getFiles() {
        return files;
    }

    public File getRootDir() {
        return rootDir;
    }


    @Override
    public void close() {
        FileUtils.deleteQuietly(rootDir);
    }
}
