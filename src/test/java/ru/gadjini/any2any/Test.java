package ru.gadjini.any2any;

import com.aspose.imaging.Image;
import com.aspose.imaging.imageloadoptions.SvgLoadOptions;
import com.aspose.imaging.imageoptions.BmpOptions;

public class Test {

    public static void main(String[] args) throws Exception {
        try (Image image = Image.load("C:/images/love-and-romance.svg", new SvgLoadOptions())) {
            image.save("C:/images/result.bmp", new BmpOptions());
        }
    }
}
