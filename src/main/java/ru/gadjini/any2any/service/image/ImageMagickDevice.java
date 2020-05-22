package ru.gadjini.any2any.service.image;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

@Component
public class ImageMagickDevice implements ImageDevice {

    @Override
    public void convert(String in, String out) {
        new ProcessExecutor().execute(getCommand(in, out), 5);
    }

    private String getCommand(String in, String out) {
        return "magick convert -background none " + in + " " + out;
    }
}
