package ru.gadjini.any2any.service.archive;

import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.Set;

public abstract class BaseArchiveDevice implements ArchiveDevice {

    private final Set<Format> availableFormats;

    protected BaseArchiveDevice(Set<Format> availableFormats) {
        this.availableFormats = availableFormats;
    }

    @Override
    public final boolean accept(Format format) {
        return availableFormats.contains(format);
    }

}
