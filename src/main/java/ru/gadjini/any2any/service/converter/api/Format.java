package ru.gadjini.any2any.service.converter.api;

public enum Format {

    DOC,
    DOCX,
    RTF,
    PDF,
    PNG,
    JPEG,
    JPG,
    TXT,
    EPUB,
    DEVICE_PHOTO {
        @Override
        public String getExt() {
            return "png";
        }
    },
    URL,
    TEXT;

    public String getExt() {
        return name().toLowerCase();
    }
}
