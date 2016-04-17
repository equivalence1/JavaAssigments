package task.client;

import task.GlobalFunctions;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */

//means File System Handler
public class FSHandler {
    private static final int BUFFER_SIZE = 1000000;

    public static void handleList(String path, DataOutputStream out) {
        Path dir;
        try {
            dir = FileSystems.getDefault().getPath(path);
        } catch (InvalidPathException e) {
            try {
                out.writeLong(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return;
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
            GlobalFunctions.printError("Error while listing directory");
            try {
                out.writeLong(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return;
        }

        try {
            out.writeLong(names.size());

            for (int i = 0; i < names.size(); i++) {
                out.writeUTF(names.get(i));
                out.writeBoolean(isDir.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleGet(String path, DataOutputStream out) {
        Path file;
        try {
            file = FileSystems.getDefault().getPath(path);
        } catch (InvalidPathException e) {
            try {
                out.writeLong(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(file.toFile());
                BufferedInputStream in = new BufferedInputStream(fis)) {
            out.writeLong(file.toFile().getTotalSpace());

            byte[] ioBuf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(ioBuf)) != -1){
                out.write(ioBuf, 0, bytesRead);
            }
        } catch (IOException | DirectoryIteratorException e) {
            GlobalFunctions.printError("Error while getting file");
            e.printStackTrace();
        }
    }
}
