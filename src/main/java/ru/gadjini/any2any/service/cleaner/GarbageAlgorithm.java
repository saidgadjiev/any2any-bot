package ru.gadjini.any2any.service.cleaner;

import java.io.File;

public interface GarbageAlgorithm {

    boolean accept(File file);

    boolean isGarbage(File file);
}
