package ru.gadjini.any2any.service.unzip;

import com.github.junrar.Junrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.WindowsCondition;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.service.converter.api.Format;

import java.io.File;
import java.util.Set;

@Component
@Conditional(WindowsCondition.class)
public class RarZipService extends BaseZipService {

    @Autowired
    public RarZipService() {
        super(Set.of(Format.RAR));
    }

    public void unzip(int userId, String in, String out) {
        try {
            Junrar.extract(new File(in), new File(out));
        } catch (Exception e) {
            throw new UnzipException(e);
        }
    }
}
