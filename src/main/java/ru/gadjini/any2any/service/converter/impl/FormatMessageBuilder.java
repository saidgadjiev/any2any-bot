package ru.gadjini.any2any.service.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FormatMessageBuilder {

    private static final Set<Format> IGNORE_FORMATS = Set.of(Format.DEVICE_PHOTO);

    private FormatService formatService;

    @Autowired
    public FormatMessageBuilder(FormatService formatService) {
        this.formatService = formatService;
    }

    public String formats(FormatCategory category) {
        Map<List<Format>, List<Format>> formats = formatService.getFormats(category);
        StringBuilder msg = new StringBuilder();

        for (Map.Entry<List<Format>, List<Format>> entry : formats.entrySet()) {
            if (msg.length() > 0) {
                msg.append("\n");
            }
            String left = entry.getKey().stream().filter(format -> !IGNORE_FORMATS.contains(format)).map(Format::name).collect(Collectors.joining(", "));
            String right = entry.getValue().stream().filter(format -> !IGNORE_FORMATS.contains(format)).map(Format::name).collect(Collectors.joining(", "));

            msg.append(left).append(" - ").append(right);
        }

        return msg.toString();
    }
}
