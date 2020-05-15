package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Set;

public abstract class BaseZipService implements ZipService {

    private final Set<Format> availableFormats;

    protected BaseZipService(Set<Format> availableFormats) {
        this.availableFormats = availableFormats;
    }

    @Override
    public final boolean accept(Format format) {
        return availableFormats.contains(format);
    }

}
