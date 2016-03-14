package task;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */

//means File System Handler
public class FSHandler {
    public static String list(String path) {
        Path dir;
        try {
            dir = FileSystems.getDefault().getPath(path);
        } catch (InvalidPathException e) {
            return "0";
        }

        List<String> names = new ArrayList<>();
        List<Boolean> isDir = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path filePath: stream) {
                File file = filePath.toFile();

                names.add(file.getName());
                isDir.add(file.isDirectory());
            }
        } catch (IOException | DirectoryIteratorException x) {
            GlobalNamespace.printError("Error while listing directory");
            return "0";
        }

        return constructResultList(names, isDir);
    }

    public static String get(String path) {
        Path file;
        try {
            file = FileSystems.getDefault().getPath(path);
        } catch (InvalidPathException e) {
            return "0";
        }

        try {
            List<String> lines = Files.readAllLines(file);
            return constructResultGet(lines);
        } catch (IOException | DirectoryIteratorException x) {
            GlobalNamespace.printError("Error while listing directory");
            return "0";
        }
    }

    private static String constructResultList(List<String> names, List<Boolean> isDir) {
        StringBuilder result = new StringBuilder();
        String toAppend = GlobalNamespace.BLUE + names.size() + GlobalNamespace.RESET;
        result.append(toAppend);

        for (int i = 0; i < names.size(); i++) {
            if (i != names.size() - 1)
                toAppend = newEntry(names.get(i), isDir.get(i), ",");
            else
                toAppend = newEntry(names.get(i), isDir.get(i), "");
            result.append(toAppend);
        }

        return result.toString();
    }

    private static String newEntry(String name, Boolean isDir, String suffix) {
        return " (" + GlobalNamespace.GREEN + name + GlobalNamespace.RESET + " " +
                GlobalNamespace.YELLOW + isDir + GlobalNamespace.RESET + ")" + suffix;
    }

    private static String constructResultGet(List<String> lines) {
        StringBuilder result = new StringBuilder();
        long size = lines.stream().map(String::length).collect(Collectors.summarizingInt(x -> x)).getSum();
        String toAppend = GlobalNamespace.BLUE + "<size: " + size + ">" + GlobalNamespace.RESET + " ";
        result.append(toAppend);

        lines.forEach(result::append);

        return result.toString();
    }
}
