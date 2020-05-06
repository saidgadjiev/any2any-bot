package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.property.WkhtmltopdfProperties;

import java.util.ArrayList;
import java.util.List;

@Service
public class WkhtmltopdfService {

    private WkhtmltopdfProperties wkhtmltopdfProperties;

    @Autowired
    public WkhtmltopdfService(WkhtmltopdfProperties wkhtmltopdfProperties) {
        this.wkhtmltopdfProperties = wkhtmltopdfProperties;
    }

    public void process(String url, String out) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args(url, out));
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

    private String[] args(String url, String out) {
        List<String> args = new ArrayList<>();
        args.add(wkhtmltopdfProperties.getExecution());
        args.add("--load-error-handling");
        args.add("abort");
        args.add("--load-media-error-handling");
        args.add("abort");
        args.add(url);
        args.add(out);

        return args.toArray(new String[0]);
    }
}
