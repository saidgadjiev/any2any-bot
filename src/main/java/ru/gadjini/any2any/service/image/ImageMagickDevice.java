package ru.gadjini.any2any.service.image;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

@Component
public class ImageMagickDevice implements ImageDevice {

    @Override
    public void convert(String in, String out, String... options) {
        new ProcessExecutor().execute(getCommand(in, out, options), 10);
    }

    private String getCommand(String in, String out, String... options) {
        StringBuilder command = new StringBuilder();
        command.append("convert -background none ");
        for (String option : options) {
            command.append(option).append(" ");
        }

        command.append(in).append(" ").append(out);

        return command.toString();
    }
}
