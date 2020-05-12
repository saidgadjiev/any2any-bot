package ru.gadjini.any2any.service.converter.impl;

import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;

import java.util.Set;

public abstract class BaseAny2AnyConverter<T extends ConvertResult> implements Any2AnyConverter<ConvertResult> {

    private final Set<Format> availableFormats;

    FormatService formatService;

    protected BaseAny2AnyConverter(Set<Format> availableFormats, FormatService formatService) {
        this.availableFormats = availableFormats;
        this.formatService = formatService;
    }

    @Override
    public final boolean accept(Format format, Format targetFormat) {
        return availableFormats.contains(format) && formatService.isConvertAvailable(format, targetFormat);
    }
}
