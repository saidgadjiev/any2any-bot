package ru.gadjini.any2any.service.converter.api.result;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class FileResult implements ConvertResult {

    private final File file;

    public FileResult(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public ResultType getResultType() {
        return ResultType.FILE;
    }

    @Override
    public void close() {
        FileUtils.deleteQuietly(file);
    }
}
