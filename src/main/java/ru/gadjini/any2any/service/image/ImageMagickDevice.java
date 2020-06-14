package ru.gadjini.any2any.service.image;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageDevice {

    @Override
    public void convert(String in, String out, String... options) {
        new ProcessExecutor().execute(getCommand(in, out, options), 2 * 60);
    }

    private String[] getCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>(commandName());
        command.add("-background");
        command.add("none");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private List<String> commandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "convert") : List.of("convert");
    }
}
