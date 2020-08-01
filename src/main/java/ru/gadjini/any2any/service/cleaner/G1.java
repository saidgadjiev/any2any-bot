package ru.gadjini.any2any.service.cleaner;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.utils.FileUtils2;

import java.io.File;

@Component
public class G1 implements GarbageAlgorithm {

    @Override
    public boolean accept(File file) {
        return true;
    }

    @Override
    public boolean isGarbage(File file) {
        return FileUtils2.isExpired(file, 1);
    }
}
