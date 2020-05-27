package ru.gadjini.any2any.service;

import org.springframework.stereotype.Service;

@Service
public class WkhtmltopdfService {

    public void process(String urlOrHtml, String out) {
        new ProcessExecutor().execute(buildCommand(urlOrHtml, out), 10);
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
