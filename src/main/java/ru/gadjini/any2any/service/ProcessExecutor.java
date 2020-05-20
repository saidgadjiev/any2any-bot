package ru.gadjini.any2any.service;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gadjini.any2any.exception.ProcessException;
import ru.gadjini.any2any.exception.UnzipException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    public void execute(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try {
                boolean result = process.waitFor(10, TimeUnit.SECONDS);
                if (!result) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    throw new RuntimeException(error);
                }
                if (process.exitValue() != 0) {
                    String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                    LOGGER.error("Exit code " + process.exitValue() + " out: " + error + ". Command: " + command);
                }
            } finally {
                process.destroy();
            }
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }
}
