package ru.gadjini.any2any.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gadjini.any2any.exception.ProcessException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    public String execute(String[] command, int timeout) {
        try {
            Process process = new ProcessBuilder(command).start();
            try {
                boolean result = process.waitFor(timeout, TimeUnit.SECONDS);
                if (!result) {
                    throw new RuntimeException("Timed out. Command " + Arrays.toString(command));
                }
                if (process.exitValue() != 0) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    if (StringUtils.isBlank(error)) {
                        error = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                    }
                    LOGGER.error("Exit code " + process.exitValue() + " out: " + error + ". Command: " + Arrays.toString(command));
                }

                return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            } finally {
                process.destroy();
            }
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }
}
