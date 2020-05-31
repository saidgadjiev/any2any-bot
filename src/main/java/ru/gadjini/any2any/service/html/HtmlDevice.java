package ru.gadjini.any2any.service.html;

public interface HtmlDevice {

    void processHtml(String urlOrHtml, String out);

    void processUrl(String url, String out);
}
