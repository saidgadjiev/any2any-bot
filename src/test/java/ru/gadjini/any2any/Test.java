package ru.gadjini.any2any;

import com.aspose.pdf.Document;
import com.aspose.pdf.EpubLoadOptions;
import com.aspose.pdf.SaveFormat;
import com.aspose.words.License;
import org.springframework.core.io.ClassPathResource;

public class Test {

    public static void main(String[] args) throws Exception {
        applyLicenses();
        Document epub = new Document("C:/test.epub", new EpubLoadOptions());
        try {
            epub.save("C:/test.doc", SaveFormat.Doc);
        } finally {
            epub.dispose();
        }
        com.aspose.words.Document doc = new com.aspose.words.Document("C:/test.doc");
        try {
            doc.save("C:/test.rtf", com.aspose.words.SaveFormat.RTF);
        } finally {
            doc.cleanup();
        }
    }

    private static void applyLicenses() {
        try {
            new License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
            new com.aspose.pdf.License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
            new com.aspose.imaging.License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
