package ru.gadjini.any2any.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartLogger {

    private final Logger logger;

    public SmartLogger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    public void debug(String method, Object... args) {
        logger.debug(method + "(" + StringUtils.repeat("{}", args.length) + ")", args);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void error(String method, Object... args) {
        logger.error(method + "(" + StringUtils.repeat("{}", args.length) + ")", args);
    }

    public void error(String message, Throwable e) {
        logger.error(message, e);
    }


    public void error(String message) {
        logger.error(message);
    }

    public Logger getLogger() {
        return logger;
    }
}
