package ru.gadjini.any2any.service.conversion.device;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.ProcessExecutor;

@Component
@Qualifier("calibre")
public class CalibreDevice implements ConvertDevice {

    @Override
    public void convert(String in, String out) {
        new ProcessExecutor().execute(buildCommand(in, out));
    }

    private String[] buildCommand(String in, String out) {
        return new String[] {
                "ebook-convert",
                in,
                out
        };
    }
}
