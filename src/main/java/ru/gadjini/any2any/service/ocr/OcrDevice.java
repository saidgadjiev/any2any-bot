package ru.gadjini.any2any.service.ocr;

import java.io.IOException;

public interface OcrDevice {

    String getText(String filePath) throws IOException;
}
