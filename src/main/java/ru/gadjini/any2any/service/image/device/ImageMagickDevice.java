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
    public void transparent(String in, String out, String color, boolean remove) {
        if (remove) {
            new ProcessExecutor().execute(getTransparentRemoveCommand(in, out, color), 10);
        } else {
            new ProcessExecutor().execute(getTransparentExcludeCommand(in, out, color), 10);
        }
    }

    private String[] getTransparentExcludeCommand(String in, String out, String color) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("-matte");
        command.add("( +clone -fuzz 10% -transparent " + color + " )");
        command.add("-compose");
        command.add("DstOut");
        command.add("-composite");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getTransparentRemoveCommand(String in, String out, String color) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("-fuzz");
        command.add("10%");
        command.add("-transparent");
        command.add(color);
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
