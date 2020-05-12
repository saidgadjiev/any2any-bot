package ru.gadjini.any2any.service.converter.api;

public enum Format {

    DOC,
    DOCX,
    RTF,
    PDF,
    PNG,
    SVG,
    JPEG,
    JPG,
    BMP,
    TXT,
    TIFF,
    EPUB,
    WEBP,
    STICKER {
        @Override
        public String getExt() {
            return "webp";
        }
    },
    DEVICE_PHOTO {
        @Override
        public String getExt() {
            return "png";
        }
    },
    HTML,
    URL,
    TEXT;

    public String getExt() {
        return name().toLowerCase();
    }
}
