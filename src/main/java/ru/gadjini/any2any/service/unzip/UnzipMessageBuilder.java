package ru.gadjini.any2any.service.unzip;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;

@Service
public class UnzipMessageBuilder {

    public String getFilesList(Collection<String> files) {
        StringBuilder message = new StringBuilder();
        int i = 1;
        for (Iterator<String> iterator = files.iterator(); iterator.hasNext();) {
            message.append(i++).append(") ").append(iterator.next());
            if (iterator.hasNext()) {
                message.append("\n");
            }
        }

        return message.toString();
    }
}
