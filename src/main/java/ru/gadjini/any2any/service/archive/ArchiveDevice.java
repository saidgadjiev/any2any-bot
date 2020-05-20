package ru.gadjini.any2any.service.archive;

import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;

public interface ArchiveDevice {

    void zip(List<String> files, String out);

    boolean accept(Format format);
}
