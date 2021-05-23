package wocken;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.*;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.*;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import wocken.json.GoogleMetadata;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Processor {

    private static final String inputPathParentDir = "/Users/joewocken/Desktop/jpeg-testing";
    private static final String outputPathParentDir = "/Users/joewocken/Desktop/jpeg-testing/output";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        try {
            List<String> files = findFiles(Paths.get(inputPathParentDir), "jpg");
            files.forEach((String filePath) -> {
                System.out.println(filePath);
                File image = new File(filePath);
                if (!image.exists()) {
                    return;
                }
                System.out.println(image.getName());
                String imageFileName = image.getName();
                String jsonImageFilePath = inputPathParentDir + "/" + imageFileName + ".json";
                System.out.println(jsonImageFilePath);
                File jsonFile = new File(jsonImageFilePath);
                if (!jsonFile.exists()) {
                    return;
                }
                GoogleMetadata googleMetadata;
                try {
                    googleMetadata = mapper.readValue(jsonFile, GoogleMetadata.class);
                    System.out.println(mapper.writeValueAsString(googleMetadata));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (null == googleMetadata
                        || null == googleMetadata.getPhotoTakenTime()
                        || null == googleMetadata.getPhotoTakenTime().getTimestamp()) {
                    System.out.println("photo taken time doesn't exist in json");
                    return;
                }
                Integer photoTakenTimestamp = googleMetadata.getPhotoTakenTime().getTimestamp();
                OffsetDateTime photoTakenDateTime = OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(photoTakenTimestamp),
                        ZoneId.of("UTC")
                );

                Path outputPath = Paths.get(outputPathParentDir + "/" + imageFileName);


                ImageMetadata metadata = null;
                try {
                    metadata = Imaging.getMetadata(image);
                } catch (ImageReadException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                JpegImageMetadata jpegImageMetadata = (JpegImageMetadata) metadata;
                if (null != jpegImageMetadata) {
                    TiffField dateTimeTiffField = getAndPrintTiffField(jpegImageMetadata, TiffTagConstants.TIFF_TAG_DATE_TIME);
                    TiffField dateTimeOriginalTiffField = getAndPrintTiffField(jpegImageMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
//            printTagValue(jpegImageMetadata, TiffTagConstants.TIFF_TAG_DATE_TIME);

                    final TiffImageMetadata exif = jpegImageMetadata.getExif();
                    TiffOutputSet outputSet = null;
                    try {
                        outputSet = exif.getOutputSet();
                    } catch (ImageWriteException e) {
                        e.printStackTrace();
                    }
                    if (null == outputSet) {
                        outputSet = new TiffOutputSet();
                    }
                    try {
                        final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                    } catch (ImageWriteException e) {
                        e.printStackTrace();
                    }
//            TagInfoAscii dateTimeTiffTagInfo = (TagInfoAscii) dateTimeTiffField.getTagInfo();
//            TagInfoAscii dateTimeOriginalTiffTagInfo = (TagInfoAscii) dateTimeOriginalTiffField.getTagInfo();
//            exifDirectory.add(dateTimeTiffTagInfo, "1996:03:27-01:02:03");
//            exifDirectory.add(dateTimeOriginalTiffTagInfo, "1996:03:27-01:02:03");
                    OutputStream outputStream = null;
                    try {
                        outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    AtomicBoolean foundGpsDateStampField = new AtomicBoolean(false);
                    outputSet.getDirectories().forEach((TiffOutputDirectory dir) -> {
                        dir.getFields().forEach((TiffOutputField field) -> {
                            if (field.tagInfo.name.equalsIgnoreCase("GPSDateStamp")) {
                                foundGpsDateStampField.set(true);
                                System.out.println("GPSDateStamp output field: " + field);
                                TagInfoAscii tagInfo = (TagInfoAscii) field.tagInfo;
                                dir.removeField(tagInfo);
                                TagInfoAscii newTagInfo = new TagInfoAscii(
                                        tagInfo.name,
                                        tagInfo.tag,
                                        tagInfo.length,
                                        tagInfo.directoryType
                                );
                                String dateValue = getStringDate(photoTakenDateTime);
                                try {
                                    dir.add(newTagInfo, dateValue);
                                } catch (ImageWriteException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (field.tagInfo.name.equalsIgnoreCase("DateTimeOriginal")) {
                                System.out.println("DateTimeOriginal output field: " + field);
                                TagInfoAscii tagInfo = (TagInfoAscii) field.tagInfo;
                                dir.removeField(tagInfo);
                                TagInfoAscii newTagInfo = new TagInfoAscii(
                                        tagInfo.name,
                                        tagInfo.tag,
                                        tagInfo.length,
                                        tagInfo.directoryType
                                );
                                try {
                                    dir.add(newTagInfo, getStringDateAndTime(photoTakenDateTime));
                                } catch (ImageWriteException e) {
                                    e.printStackTrace();
                                }
                            }

                            //TODO: can't find (even tho in exiftool output)
                            if (field.tagInfo.name.equalsIgnoreCase("CreateDate")) {
                                System.out.println("CreateDate output field: " + field);
                                TagInfoAscii tagInfo = (TagInfoAscii) field.tagInfo;
                                dir.removeField(tagInfo);
                                TagInfoAscii newTagInfo = new TagInfoAscii(
                                        tagInfo.name,
                                        tagInfo.tag,
                                        tagInfo.length,
                                        tagInfo.directoryType
                                );
                                try {
                                    dir.add(newTagInfo, getStringDateAndTime(photoTakenDateTime));
                                } catch (ImageWriteException e) {
                                    e.printStackTrace();
                                }
                            }

                            //TODO: can't find (even tho in exiftool output)
                            if (field.tagInfo.name.equalsIgnoreCase("ModifyDate")) {
                                System.out.println("ModifyDate output field: " + field);
                                TagInfoAscii tagInfo = (TagInfoAscii) field.tagInfo;
                                dir.removeField(tagInfo);
                                TagInfoAscii newTagInfo = new TagInfoAscii(
                                        tagInfo.name,
                                        tagInfo.tag,
                                        tagInfo.length,
                                        tagInfo.directoryType
                                );
                                try {
                                    dir.add(newTagInfo, getStringDateAndTime(photoTakenDateTime));
                                } catch (ImageWriteException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (field.tagInfo.name.equalsIgnoreCase("DateTime")) {
                                System.out.println("DateTime output field: " + field);
                                TagInfoAscii tagInfo = (TagInfoAscii) field.tagInfo;
                                dir.removeField(tagInfo);
                                TagInfoAscii newTagInfo = new TagInfoAscii(
                                        tagInfo.name,
                                        tagInfo.tag,
                                        tagInfo.length,
                                        tagInfo.directoryType
                                );
                                try {
                                    dir.add(newTagInfo, getStringDateAndTime(photoTakenDateTime));
                                } catch (ImageWriteException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    });
                    if (!foundGpsDateStampField.get()) {
                        System.out.println("didn't find the gps date stamp field in photo");
                        TiffOutputDirectory foundTiffOutputDir = null;
                        try {
                            foundTiffOutputDir = outputSet.getOrCreateGPSDirectory();
                        } catch (ImageWriteException e) {
                            e.printStackTrace();
                        }
                        if (null != foundTiffOutputDir) {
                            System.out.println("found the tiff output directory");
                            TagInfoAscii gpsDateStampTagInfoAscii = new TagInfoAscii(
                                    "GPSDateStamp",
                                    29,
                                    11,
                                    TiffDirectoryType.EXIF_DIRECTORY_GPS
                            );
                            try {
                                foundTiffOutputDir.add(gpsDateStampTagInfoAscii, getStringDate(photoTakenDateTime));
                            } catch (ImageWriteException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("didn't find the tiff output directory");
                        }
                    }
                    ExifRewriter exifRewriter = new ExifRewriter();
                    try {
                        exifRewriter.updateExifMetadataLossless(image, outputStream, outputSet);
                    } catch (ImageReadException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ImageWriteException e) {
                        e.printStackTrace();
                    }
                }
                System.exit(0);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

//        File image = new File("C:\\Users\\josep\\Desktop\\jpeg-testing\\IMG_4676.jpg");
//        Path outputPath = Paths.get("C:\\Users\\josep\\Desktop\\jpeg-testing\\IMG_4676_output.jpg");




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

    private static TiffField getAndPrintTiffField(final JpegImageMetadata jpegMetadata,
                                           final TagInfo tagInfo) {
        final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
        if (field == null) {
            System.out.println(tagInfo.name + ": " + "Not Found.");
        } else {
            System.out.println(tagInfo.name + ": "
                    + field.getValueDescription());
        }
        return field;
    }

    /*
        "1996:03:27-01:02:03"
     */
    private static String getStringDateAndTime(OffsetDateTime dateTime) {
        int hour = dateTime.getHour();
        int min = dateTime.getMinute();
        int sec = dateTime.getSecond();
        return getStringDate(dateTime) + "-" + hour + ":" + min + ":" + sec;
    }

    /*
        returns 'YYYY:MM:dd'
     */
    private static String getStringDate(OffsetDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();
        return year + ":" + month + ":" + day;
    }

}
