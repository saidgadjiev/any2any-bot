package ru.gadjini.any2any.service.converter.api.result;

import ru.gadjini.any2any.io.SmartTempFile;

import java.io.File;

public class FileResult extends BaseConvertResult {

    private final SmartTempFile file;

    public FileResult(SmartTempFile file, long time) {
        super(time);
        this.file = file;
    }

    public File getFile() {
        return file.getFile();
    }

    @Override
    public ResultType resultType() {
        return ResultType.FILE;
    }

    @Override
    public void close() {
        file.smartDelete();
    }
}
