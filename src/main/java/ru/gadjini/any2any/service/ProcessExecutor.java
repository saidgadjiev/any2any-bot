package ru.gadjini.any2any.service;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gadjini.any2any.exception.ProcessException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    public String executeWithResult(String[] command) {
        return execute(command, ProcessBuilder.Redirect.PIPE);
    }

    public void execute(String[] command) {
        execute(command, ProcessBuilder.Redirect.DISCARD);
    }

    public String execute(String[] command, ProcessBuilder.Redirect redirect) {
        try {
            Process process = new ProcessBuilder(command).redirectOutput(redirect).start();
            try {
                int exitValue = process.waitFor();
                if (exitValue != 0) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);

                    LOGGER.error("Error({}, {}, {})", process.exitValue(), Arrays.toString(command), error);
                }

                if (redirect == ProcessBuilder.Redirect.PIPE) {
                    return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                }

                return null;
            } finally {
                process.destroy();
            }
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }
}
