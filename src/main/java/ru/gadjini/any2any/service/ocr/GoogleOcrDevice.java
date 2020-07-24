package ru.gadjini.any2any.service.ocr;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleOcrDevice implements OcrDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOcrDevice.class);

    @Override
    public String getText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) {
                return null;
            }
            AnnotateImageResponse imageResponse = responses.get(0);
            if (imageResponse.hasError()) {
                LOGGER.error("Error: {}", imageResponse.getError().getMessage());
                return null;
            }

            return imageResponse.getFullTextAnnotation().getText();
        }
    }
}
