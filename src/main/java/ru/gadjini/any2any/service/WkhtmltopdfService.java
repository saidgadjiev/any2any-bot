package ru.gadjini.any2any.service;

import com.aspose.words.Run;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.property.WkhtmltopdfProperties;

import java.io.IOException;

@Service
public class WkhtmltopdfService {

    private WkhtmltopdfProperties wkhtmltopdfProperties;

    @Autowired
    public WkhtmltopdfService(WkhtmltopdfProperties wkhtmltopdfProperties) {
        this.wkhtmltopdfProperties = wkhtmltopdfProperties;
    }

    public void process(String url, String out) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(wkhtmltopdfProperties.getExecution(), url, out);
            Process process = processBuilder.start();
            try {
                int code = process.waitFor();
                if (code != 0) {
                    throw new RuntimeException(String.valueOf(code));
                }
            } finally {
                process.destroy();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
