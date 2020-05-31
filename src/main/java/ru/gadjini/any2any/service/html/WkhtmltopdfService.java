package ru.gadjini.any2any.service.html;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.ProcessExecutor;

@Service
@Qualifier("wkhtml")
public class WkhtmltopdfService implements HtmlDevice {

    public void processHtml(String html, String out) {
        new ProcessExecutor().execute(buildCommand(html, out), 10);
    }

    @Override
    public void processUrl(String url, String out) {
        new ProcessExecutor().execute(buildCommand(url, out), 10);
    }

    private String[] buildCommand(String urlOrHtml, String out) {
        return new String[]{
                "wkhtmltopdf",
                "--no-pdf-compression",
                "--disable-local-file-access",
                "--disable-internal-links",
                "--load-error-handling",
                "ignore",
                "--load-media-error-handling",
                "ignore",
                urlOrHtml,
                out
        };
    }
}
