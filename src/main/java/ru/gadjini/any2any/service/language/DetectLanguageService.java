package ru.gadjini.any2any.service.language;

import com.detectlanguage.DetectLanguage;
import com.detectlanguage.errors.APIError;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.property.DetectLanguageProperties;

@Service
public class DetectLanguageService implements LanguageDetector {

    private static final SmartLogger LOGGER = new SmartLogger(DetectLanguageService.class);

    @Autowired
    public DetectLanguageService(DetectLanguageProperties detectLanguageProperties) {
        DetectLanguage.apiKey = detectLanguageProperties.getKey();
    }

    @Override
    public String detect(String text) {
        try {
            String language = DetectLanguage.simpleDetect(text);

            if (StringUtils.isBlank(language)) {
                LOGGER.debug("Language not detected", StringUtils.substring(text, 50));
            }

            return language;
        } catch (APIError apiError) {
            LOGGER.error(apiError.getMessage() + " code " + apiError.code, apiError);
            return null;
        }
    }
}
