package wocken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReductiveFileCleanup {
    private static final String inputPathParentDir = "/Users/joewocken/Desktop/jpeg-testing/delete-originals";

    public static void main(String[] args) throws Exception {
        List<String> files = findFiles(Paths.get(inputPathParentDir));
        Map<String, File> fileNameToFileMap = new HashMap<>();
        for (String filePath : files) {
            File image = new File(filePath);
            if (image.exists()) {
                String imageFileName = image.getName();
                fileNameToFileMap.put(imageFileName, image);
            }
        }
        System.out.println(fileNameToFileMap);
        Map<String, File> editedFilesToRename = new HashMap<>();
        List<File> filesToDelete = new ArrayList<>();
        fileNameToFileMap.forEach((String fileName, File file) -> {
            if (fileName.contains("edited")) {
                // 1. delete the original file
                String originalFileName = fileName.replace("-edited", "");
                filesToDelete.add(new File(file.getParent() + "/" + originalFileName));
                // 2. rename the edited version to the original file name
                editedFilesToRename.put(fileName, new File(fileName));
            }
        });
        filesToDelete.forEach((File fileToDelete) -> {
            if (fileToDelete.exists()) {
                boolean deleted = fileToDelete.delete();
                if (!deleted) {
                    System.out.println(fileToDelete + " not deleted");
                }
            }
        });

    }

    public static List<String> findFiles(Path path)
            throws IOException {

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }

        List<String> result;

        try (Stream<Path> walk = Files.walk(path, 1)) {
            result = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
//                    .map(p -> p.toString().toLowerCase())
                    .collect(Collectors.toList());
        }

        return result;
    }
}
