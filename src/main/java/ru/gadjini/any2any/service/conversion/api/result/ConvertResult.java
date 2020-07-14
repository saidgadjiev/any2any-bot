package ru.gadjini.any2any.service.conversion.api.result;

public interface ConvertResult extends AutoCloseable {

    ResultType resultType();

    long time();
}
