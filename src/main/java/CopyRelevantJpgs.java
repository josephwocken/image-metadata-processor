import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import wocken.json.GoogleMetadata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyRelevantJpgs {

    private static final String inputPathParentDir = "/Users/joewocken/Desktop/jpeg-testing";
    private static final String outputPathParentDir = "/Users/joewocken/Desktop/jpeg-testing/output";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        List<String> files = findFiles(Paths.get(inputPathParentDir), "jpg");
        Map<String, File> fileNameToFileMap = new HashMap<>();
        for (String filePath : files) {
            File image = new File(filePath);
            if (image.exists()) {
                String imageFileName = image.getName();
                fileNameToFileMap.put(imageFileName, image);
            }
        }
        System.out.println(fileNameToFileMap);
        Map<String, File> fileNameToFileMap2 = new HashMap<>();
        fileNameToFileMap.forEach((String fileName, File file) -> {
            if (!fileName.contains("edited")) {
                String fileNameWithEdited = fileName.replace(".jpg", "-edited.jpg");
                File editedFile = fileNameToFileMap.get(fileNameWithEdited);
                if (null != editedFile) {
                    fileNameToFileMap2.put(fileName, editedFile);
                } else {
                    fileNameToFileMap2.put(fileName, file);
                }
            }
        });
        System.out.println(fileNameToFileMap2);
        fileNameToFileMap2.forEach((String fileName, File file) -> {
            Path outputPath = Paths.get(outputPathParentDir + "/" + fileName);
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                Files.write(outputPath, fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        List<String> jsonFiles = findFiles(Paths.get(inputPathParentDir), "json");
        jsonFiles.forEach((String jsonFile) -> {
            File file = new File(jsonFile);
            Path outputPath = Paths.get(outputPathParentDir + "/" + jsonFile);
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                Files.write(outputPath, fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<String> findFiles(Path path, String fileExtension)
            throws IOException {

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }

        List<String> result;

        try (Stream<Path> walk = Files.walk(path, 1)) {
            result = walk
                    .filter(p -> !Files.isDirectory(p))
                    // this is a path, not string,
                    // this only test if path end with a certain path
                    //.filter(p -> p.endsWith(fileExtension))
                    // convert path to string first
                    .map(p -> p.toString().toLowerCase())
                    .filter(f -> f.endsWith(fileExtension))
                    .collect(Collectors.toList());
        }

        return result;
    }
}
