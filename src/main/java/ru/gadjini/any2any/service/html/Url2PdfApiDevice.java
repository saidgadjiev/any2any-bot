package ru.gadjini.any2any.service.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.property.ConversionProperties;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.utils.UrlUtils;

@Service
@Qualifier("api")
public class Url2PdfApiDevice implements HtmlDevice {

    private ConversionProperties conversionProperties;

    @Autowired
    public Url2PdfApiDevice(ConversionProperties conversionProperties) {
        this.conversionProperties = conversionProperties;
    }

    @Override
    public void processHtml(String html, String out) {
        new ProcessExecutor().execute(buildCommandByHtml(html, out), 70);
    }

    @Override
    public void processUrl(String url, String out) {
        new ProcessExecutor().execute(buildCommandByUrl(UrlUtils.appendScheme(url), out), 70);
    }

    private String[] buildCommandByUrl(String url, String out) {
        return new String[]{"curl", "-XGET", getConversionUrlByUrl(url), "-o", out};
    }

    private String[] buildCommandByHtml(String html, String out) {
        return new String[]{"curl", "-XPOST", "-d@" + html, "-H", "content-type: text/html", getBaseApi(), "-o", out};
    }

    private String getConversionUrlByUrl(String url) {
        return getBaseApi() + "?url=" + url + "&ignoreHttpsErrors=true&goto.timeout=60000&goto.waitUntil=load&pdf.height=auto";
    }

    private String getBaseApi() {
        return conversionProperties.getServer() + "/api/render";
    }
}
