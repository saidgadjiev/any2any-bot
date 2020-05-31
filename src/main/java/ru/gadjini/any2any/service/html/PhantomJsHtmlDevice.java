package ru.gadjini.any2any.service.html;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.ProcessExecutor;

@Service
@Qualifier("phantomjs")
public class PhantomJsHtmlDevice implements HtmlDevice {

    @Override
    public void process(String urlOrHtml, String out) {
        new ProcessExecutor().execute(buildCommand(urlOrHtml, out), 20);
    }

    private String[] buildCommand(String urlOrHtml, String out) {
        return new String[] {
                "phantomjs",
                "rasterize.js",
                urlOrHtml,
                out
        };
    }
}
