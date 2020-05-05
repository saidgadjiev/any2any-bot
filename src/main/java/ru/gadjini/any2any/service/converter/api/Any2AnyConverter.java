package ru.gadjini.any2any.service.converter.api;

import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;

public interface Any2AnyConverter<T extends ConvertResult> {

    T convert(FileQueueItem fileQueueItem);

    boolean accept(String extension);
}
