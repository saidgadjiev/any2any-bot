package ru.gadjini.any2any.service.image.device;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageDevice {

    @Override
    public void convert(String in, String out, String... options) {
        new ProcessExecutor().execute(getCommand(in, out, options), 10);
    }

    @Override
    public void negativeTransparent(String in, String out, String... colors) {
        new ProcessExecutor().execute(getTransparentRemoveCommand(in, out, true, colors), 10);
    }

    @Override
    public void positiveTransparent(String in, String out, String color) {
        new ProcessExecutor().execute(getTransparentRemoveCommand(in, out, false, color), 10);
    }

    private String[] getTransparentRemoveCommand(String in, String out, boolean negative, String... colors) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("-fuzz");
        command.add("10%");
        String sign = negative ? "-" : "+";
        for (String color : colors) {
            command.add(sign + "transparent");
            command.add(color);
        }

        command.add(out);

        return command.toArray(new String[0]);
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
