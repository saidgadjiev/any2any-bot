package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.service.converter.api.Format;

public interface Unzipper {

    UnzipResult unzip(String fileId, Format format);

    boolean accept(Format zipFormat);
}
