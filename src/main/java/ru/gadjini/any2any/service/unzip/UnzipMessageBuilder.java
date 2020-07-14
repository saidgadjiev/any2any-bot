package ru.gadjini.any2any.service.unzip;

import org.springframework.stereotype.Service;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.util.Collection;
import java.util.Iterator;

@Service
public class UnzipMessageBuilder {

    public String getFilesList(Collection<ZipFileHeader> files) {
        StringBuilder message = new StringBuilder();
        int i = 1;
        for (Iterator<ZipFileHeader> iterator = files.iterator(); iterator.hasNext();) {
            ZipFileHeader zipFileHeader = iterator.next();
            message.append(i++).append(") ").append(zipFileHeader.getSize());
            if (zipFileHeader.getSize() != 0) {
                message.append(" (").append(MemoryUtils.humanReadableByteCount(zipFileHeader.getSize())).append(")");
            }
            if (iterator.hasNext()) {
                message.append("\n");
            }
        }

        return message.toString();
    }
}
