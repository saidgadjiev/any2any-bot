package ru.gadjini.any2any.service.converter.api.result;

import java.io.File;

public class StickerResult extends FileResult {

    public StickerResult(File file, long time) {
        super(file, time);
    }

    public ResultType resultType() {
        return ResultType.STICKER;
    }
}
