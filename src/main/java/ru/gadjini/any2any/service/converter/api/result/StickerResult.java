package ru.gadjini.any2any.service.converter.api.result;

import ru.gadjini.any2any.io.SmartTempFile;

public class StickerResult extends FileResult {

    public StickerResult(SmartTempFile file, long time) {
        super(file, time);
    }

    public ResultType resultType() {
        return ResultType.STICKER;
    }
}
