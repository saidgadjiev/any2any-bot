package ru.gadjini.any2any.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageConvertDevice, ImageIdentifyDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public ImageMagickDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String... options) {
        try {
            processExecutor.execute(getCommand(in, out, options));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void negativeTransparent(String in, String out, String inaccuracy, String... colors) {
        try {
            processExecutor.execute(getTransparentRemoveCommand(in, out, true, inaccuracy, colors));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void positiveTransparent(String in, String out, String inaccuracy, String color) {
        try {
            processExecutor.execute(getTransparentRemoveCommand(in, out, false, inaccuracy, color));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyBlackAndWhiteFilter(String in, String out) {
        try {
            processExecutor.execute(getBlackAndWhiteFilterCommand(in, out));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyNegativeFilter(String in, String out) {
        try {
            processExecutor.execute(getNegativeFilterCommand(in, out));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public void applySketchFilter(String in, String out) {
        try {
            processExecutor.execute(getSketchFilterCommand(in, out));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public void resize(String in, String out, String size) {
        try {
            processExecutor.execute(getResizeCommand(in, out, size));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public void convertToThumb(String in, String out) {
        try {
            processExecutor.execute(getThumbCommand(in, out));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public String getSize(String in) {
        try {
            return processExecutor.executeWithResult(getSizeCommand(in));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    private String[] getThumbCommand(String in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add("-thumbnail");
        command.add("320x320");
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getSizeCommand(String in) {
        List<String> command = new ArrayList<>(identifyCommandName());
        command.add("-format");
        command.add("\"%wx%h\"");
        command.add(in);

        return command.toArray(new String[0]);
    }

    private String[] getResizeCommand(String in, String out, String size) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add(in);
        command.add("-resize");
        command.add(size);
        command.add("-quality");
        command.add("100");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getNegativeFilterCommand(String in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add(in);
        command.add("-channel");
        command.add("RGB");
        command.add("-negate");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getBlackAndWhiteFilterCommand(String in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add(in);
        command.add("-colorspace");
        command.add("Gray");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getSketchFilterCommand(String in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
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
        List<String> command = new ArrayList<>(convertCommandName());
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
        List<String> command = new ArrayList<>(convertCommandName());
        command.add("-background");
        command.add("none");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private List<String> identifyCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "identify") : List.of("identify");
    }

    private List<String> convertCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "convert") : List.of("convert");
    }
}
