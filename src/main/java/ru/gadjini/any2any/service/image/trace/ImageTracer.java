package ru.gadjini.any2any.service.image.trace;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

@Component
public class ImageTracer {

    public void trace(String in, String out) {
        new ProcessExecutor().execute(command(in, out), 2 * 60);
    }

    private String[] command(String in, String out) {
        return new String[]{"java", "-jar", "ImageTracer.jar", in, "outfilename", out};
    }
}
