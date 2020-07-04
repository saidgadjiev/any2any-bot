package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.Set;

public abstract class BaseUnzipDevice implements UnzipDevice {

    private final Set<Format> availableFormats;

    protected BaseUnzipDevice(Set<Format> availableFormats) {
        this.availableFormats = availableFormats;
    }

    @Override
    public final boolean accept(Format format) {
        return availableFormats.contains(format);
    }

}
