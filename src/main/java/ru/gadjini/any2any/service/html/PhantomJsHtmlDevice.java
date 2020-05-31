package ru.gadjini.any2any.service.html;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.utils.UrlUtils;

@Service
@Qualifier("phantomjs")
public class PhantomJsHtmlDevice implements HtmlDevice {

    @Override
    public void processHtml(String urlOrHtml, String out) {
        new ProcessExecutor().execute(buildCommand(urlOrHtml, out), 90);
    }

    @Override
    public void processUrl(String url, String out) {
        new ProcessExecutor().execute(buildCommand(normalizeUrl(url), out), 90);
    }

    private String normalizeUrl(String url) {
        if (UrlUtils.hasScheme(url)) {
            return url;
        }
        if (url.contains(":443")) {
            return "https://" + url;
        }

        return "http://" + url;
    }

    private String[] buildCommand(String urlOrHtml, String out) {
        return new String[]{
                isWindows() ? "phantomjs.cmd" : "phantomjs",
                "rasterize.js",
                "\"" + urlOrHtml + "\"",
                out
        };
    }

    private boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
}
