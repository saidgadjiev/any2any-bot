package ru.gadjini.any2any.service.unzip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.condition.WindowsCondition;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Locale;
import java.util.Set;

@Component
@Conditional(WindowsCondition.class)
@Qualifier("zip")
public class ZipLibUnzipProgram extends BaseUnzipProgram {

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public ZipLibUnzipProgram(LocalisationService localisationService, UserService userService) {
        super(Set.of(Format.ZIP));
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void unzip(int userId, String in, String out) {
        try {
            ZipFile zipFile = checkZip(new ZipFile(in), userService.getLocaleOrDefault(userId));
            zipFile.extractAll(out);
        } catch (ZipException e) {
            throw new UnzipException(e);
        }
    }

    private ZipFile checkZip(ZipFile zipFile, Locale locale) throws ZipException {
        if (zipFile.isEncrypted()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_ENCRYPTED, locale));
        }
        if (!zipFile.isValidZipFile()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_INVALID, locale));
        }

        return zipFile;
    }
}
