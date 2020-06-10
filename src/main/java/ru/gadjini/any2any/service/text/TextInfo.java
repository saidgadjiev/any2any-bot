package ru.gadjini.any2any.service.text;

public class TextInfo {

    private String language;

    private String languageCode;

    private Font font;

    private TextDirection direction;

    public TextInfo() {
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setDirection(TextDirection direction) {
        this.direction = direction;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public Font getFont() {
        return font;
    }

    public TextDirection getDirection() {
        return direction;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "TextInfo{" +
                "language='" + language + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", font=" + font +
                ", direction=" + direction +
                '}';
    }
}
