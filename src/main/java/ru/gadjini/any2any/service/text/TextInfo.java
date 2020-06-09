package ru.gadjini.any2any.service.text;

public class TextInfo {

    private String language;

    private Font font;

    private TextDirection direction;

    public TextInfo() {
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setDirection(TextDirection direction) {
        this.direction = direction;
    }

    public String getLanguage() {
        return language;
    }

    public Font getFont() {
        return font;
    }

    public TextDirection getDirection() {
        return direction;
    }
}
