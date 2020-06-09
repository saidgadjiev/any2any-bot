package ru.gadjini.any2any.service.text;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TextDetector {

    private static final Set<String> RL_LANGUAGES = Set.of("ar", "az", "he", "fa", "ur");

    private static final LanguageDetector DETECTOR = LanguageDetectorBuilder.fromAllBuiltInLanguages().build();

    public TextInfo detect(String text) {
        TextInfo textInfo = new TextInfo();

        Language detectedLanguage = DETECTOR.detectLanguageOf(text);
        textInfo.setLanguage(detectedLanguage.getIsoCode639_1().toString());
        textInfo.setDirection(getDirection(textInfo.getLanguage()));
        textInfo.setFont(getFont(textInfo.getLanguage()));

        return textInfo;
    }

    private Font getFont(String language) {
        if (StringUtils.isBlank(language)) {
            return Font.TIMES_NEW_ROMAN;
        }
        for (Font font : Font.values()) {
            if (font.isSupportedLanguage(language)) {
                return font;
            }
        }

        return Font.TIMES_NEW_ROMAN;
    }

    private TextDirection getDirection(String language) {
        if (StringUtils.isBlank(language)) {
            return TextDirection.LR;
        }
        if (RL_LANGUAGES.contains(language)) {
            return TextDirection.RL;
        }

        return TextDirection.LR;
    }
}
