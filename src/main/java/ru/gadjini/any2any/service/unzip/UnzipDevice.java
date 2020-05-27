package ru.gadjini.any2any.service.unzip;

import ru.gadjini.any2any.service.converter.api.Format;

public interface UnzipDevice {

    void unzip(int userId, String in, String out);

    boolean accept(Format zipFormat);
}