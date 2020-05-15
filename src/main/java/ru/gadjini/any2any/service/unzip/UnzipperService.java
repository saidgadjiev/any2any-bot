package ru.gadjini.any2any.service.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.job.UnzipperJob;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.Locale;
import java.util.Set;

@Service
public class UnzipperService {

    private Set<Unzipper> unzippers;

    private LocalisationService localisationService;

    private UnzipperJob unzipperJob;

    @Autowired
    public UnzipperService(Set<Unzipper> unzippers, LocalisationService localisationService, UnzipperJob unzipperJob) {
        this.unzippers = unzippers;
        this.localisationService = localisationService;
        this.unzipperJob = unzipperJob;
    }

    public void unzip(int userId, String fileId, Format format, Locale locale) {
        Unzipper unzipper = getCandidate(format, locale);

        unzipperJob.addJob(new UnzipperJob.UnzipJob(unzipper, fileId, userId, format));
    }

    private Unzipper getCandidate(Format format, Locale locale) {
        for (Unzipper unzipper : unzippers) {
            if (unzipper.accept(format)) {
                return unzipper;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }
}
