package ru.gadjini.any2any.service.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.condition.LinuxMacCondition;
import ru.gadjini.any2any.exception.ProcessException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Conditional({LinuxMacCondition.class})
public class P7ZipUnzipDevice extends BaseUnzipDevice {

    private static final String TAG = "p7unzip";

    private TempFileService tempFileService;

    @Autowired
    public P7ZipUnzipDevice(TempFileService tempFileService) {
        super(Set.of(Format.ZIP, Format.RAR));
        this.tempFileService = tempFileService;
    }

    @Override
    public void unzip(int userId, String in, String out) {
        new ProcessExecutor().execute(buildUnzipCommand(in, out));
    }

    @Override
    public List<ZipFileHeader> getZipFiles(String zipFile) {
        SmartTempFile txt = tempFileService.createTempFile(TAG, "txt");

        try {
            new ProcessExecutor().executeWithFile(buildContentsCommand(zipFile), txt.getFile().getAbsolutePath());

            String contents = Files.readString(txt.toPath());
            int mainContentStartIndex = contents.indexOf("----------\n") + "----------\n".length();
            String mainContent = contents.substring(mainContentStartIndex);
            String[] fileHeaders = mainContent.split("\n\n");
            List<ZipFileHeader> result = new ArrayList<>();
            for (String fileHeader : fileHeaders) {
                if (!fileHeader.startsWith("Path")) {
                    continue;
                }
                int indexOfFolder = fileHeader.indexOf("Folder = ") + "Folder = ".length();
                String folder = fileHeader.substring(indexOfFolder, indexOfFolder + 1);
                if (folder.equals("+")) {
                    continue;
                }
                int indexOfPath = fileHeader.indexOf("Path = ");
                int endLine = fileHeader.indexOf("\n");
                String path = fileHeader.substring(indexOfPath + "Path = ".length(), endLine);
                ZipFileHeader header = new ZipFileHeader();
                header.setPath(path);

                int indexOfSize = fileHeader.indexOf("Size = ");
                fileHeader = fileHeader.substring(indexOfSize);
                endLine = fileHeader.indexOf("\n");
                String size = fileHeader.substring("Size = ".length(), endLine);
                header.setSize(Long.parseLong(size));

                result.add(header);
            }

            return result;
        } catch (IOException ex) {
            throw new ProcessException(ex);
        } finally {
            txt.smartDelete();
        }
    }

    @Override
    public void unzip(String fileHeader, String archivePath, String out) {
        new ProcessExecutor().executeWithFile(buildUnzipFileCommand(fileHeader, archivePath), out);
    }

    private String[] buildContentsCommand(String in) {
        return new String[]{"7z", "l", "-slt", in};
    }

    private String[] buildUnzipFileCommand(String file, String archive) {
        return new String[]{"7z", "e", archive, "-so", "-y", file};
    }

    private String[] buildUnzipCommand(String in, String out) {
        return new String[]{"7z", "x", in, "-y", "-o" + out};
    }
}
