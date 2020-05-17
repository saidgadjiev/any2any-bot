package ru.gadjini.any2any.service;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class WkhtmltopdfService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WkhtmltopdfService.class);

    public void process(String urlOrHtml, String out) {
        try {
            Process process = Runtime.getRuntime().exec(buildCommand(urlOrHtml, out));
            try {
                boolean result = process.waitFor(25, TimeUnit.SECONDS);
                if (!result) {
                    throw new RuntimeException("Timed out");
                }
                if (process.exitValue() != 0) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    LOGGER.error("Exit code " + process.exitValue() + " out: " + error + " urlOrHtml " + urlOrHtml);
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
                .append(urlOrHtml).append(" ").append(out);

        return command.toString();
    }
}
