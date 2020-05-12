package ru.gadjini.any2any.service.converter.api;

public enum Format {

    DOC(FormatCategory.DOCUMENTS),
    DOCX(FormatCategory.DOCUMENTS),
    RTF(FormatCategory.DOCUMENTS),
    PDF(FormatCategory.DOCUMENTS),
    PNG(FormatCategory.IMAGES),
    SVG(FormatCategory.IMAGES),
    JPEG(FormatCategory.IMAGES),
    JPG(FormatCategory.IMAGES),
    BMP(FormatCategory.IMAGES),
    TXT(FormatCategory.DOCUMENTS),
    TIFF(FormatCategory.IMAGES),
    EPUB(FormatCategory.DOCUMENTS),
    WEBP(FormatCategory.IMAGES),
    STICKER(FormatCategory.IMAGES) {
        @Override
        public String getExt() {
            return "webp";
        }
    },
    DEVICE_PHOTO(FormatCategory.IMAGES) {
        @Override
        public String getExt() {
            return "png";
        }
    },
    HTML(FormatCategory.IMAGES),
    URL(FormatCategory.DOCUMENTS),
    TEXT(FormatCategory.DOCUMENTS);

    private FormatCategory category;

    Format(FormatCategory category) {
        this.category = category;
    }

    public String getExt() {
        return name().toLowerCase();
    }

    public FormatCategory getCategory() {
        return category;
    }
}
