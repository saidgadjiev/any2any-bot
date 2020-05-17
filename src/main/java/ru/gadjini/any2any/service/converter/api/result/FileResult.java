package ru.gadjini.any2any.service.converter.api.result;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class FileResult extends BaseConvertResult {

    private final File file;

    public FileResult(File file, long time) {
        super(time);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public ResultType resultType() {
        return ResultType.FILE;
    }

    @Override
    public void close() {
        FileUtils.deleteQuietly(file.getParentFile());
    }
}
