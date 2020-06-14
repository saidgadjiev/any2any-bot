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
    public void negativeTransparent(String in, String out, String inaccuracy, String... colors) {
        new ProcessExecutor().execute(getTransparentRemoveCommand(in, out, true, inaccuracy, colors), 10);
    }

    @Override
    public void positiveTransparent(String in, String out, String inaccuracy, String color) {
        new ProcessExecutor().execute(getTransparentRemoveCommand(in, out, false, inaccuracy, color), 10);
    }

    @Override
    public void applyBlackAndWhiteEffect(String in, String out) {
        new ProcessExecutor().execute(getBlackAndWhiteEffectCommand(in, out), 10);
    }

    @Override
    public void applySketchEffect(String in, String out) {
        new ProcessExecutor().execute(getSketchEffectCommand(in, out), 10);
    }

    private String[] getBlackAndWhiteEffectCommand(String in, String out) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("-colorspace");
        command.add("Gray");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getSketchEffectCommand(String in, String out) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("(");
        command.add("-clone");
        command.add("0");
        command.add("-negate");
        command.add("-blur");
        command.add("0x5");
        command.add(")");
        command.add("-compose");
        command.add("colordodge");
        command.add("-composite");
        command.add("-modulate");
        command.add("100,0,100");
        command.add("-auto-level");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getTransparentRemoveCommand(String in, String out, boolean negative, String inaccuracy, String... colors) {
        List<String> command = new ArrayList<>(commandName());
        command.add(in);
        command.add("-fuzz");
        command.add(inaccuracy + "%");
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
