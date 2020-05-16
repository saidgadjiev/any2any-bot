package ru.gadjini.any2any.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class WkhtmltopdfService {

    public void process(String urlOrHtml, String out) {
        try {
            Process process = Runtime.getRuntime().exec(buildCommand(urlOrHtml, out));
            try {
                boolean result = process.waitFor(25, TimeUnit.SECONDS);
                if (!result) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    throw new RuntimeException(error);
                }
            } finally {
                process.destroy();
            }
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String buildCommand(String urlOrHtml, String out) {
        StringBuilder command = new StringBuilder();
        command
                .append("wkhtmltopdf ")
                .append(" --no-pdf-compression --disable-local-file-access --disable-internal-links --load-error-handling ignore --load-media-error-handling ignore ")
                .append("\"").append(urlOrHtml).append("\"").append(" ")
                .append("\"").append(out).append("\"");

        return command.toString();
    }
}
