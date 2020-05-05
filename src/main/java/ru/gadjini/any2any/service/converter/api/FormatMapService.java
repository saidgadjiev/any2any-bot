package ru.gadjini.any2any.service.converter.api;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FormatMapService {

    private final Map<List<Format>, List<Format>> formats = Map.of(
            List.of(Format.DOC, Format.DOCX), List.of(Format.PDF)
    );

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<List<Format>, List<Format>> entry: formats.entrySet()) {
            if (entry.getKey().contains(srcFormat)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }
}
