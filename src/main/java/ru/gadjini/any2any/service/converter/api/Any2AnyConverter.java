package ru.gadjini.any2any.service.converter.api;

import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;

public interface Any2AnyConverter<T extends ConvertResult> {

    T convert(ConversionQueueItem fileQueueItem);

    boolean accept(Format format, Format targetFormat);
}
