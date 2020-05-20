package ru.gadjini.any2any.service.archive;

import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Set;

public abstract class BaseZipProgram implements ZipProgram {

    private final Set<Format> availableFormats;

    protected BaseZipProgram(Set<Format> availableFormats) {
        this.availableFormats = availableFormats;
    }

    @Override
    public final boolean accept(Format format) {
        return availableFormats.contains(format);
    }

}
