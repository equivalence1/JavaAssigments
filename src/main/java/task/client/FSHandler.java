package task.client;

import task.GlobalFunctions;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */
public class FSHandler {
    /**
     * I don't want to use PART_SIZE as BUFFER_SIZE as
     * PART_SIZE can be big (and actually 10MB is pretty big)
     */
    private static final int BUFFER_SIZE = 1000000;

    public static boolean doesExist(String path) {
        File f = new File(path);
        return (f.exists() && !f.isDirectory());
    }

    public static long getSize(String path) {
        if (!doesExist(path)) {
            return 0;
        } else {
            File f = new File(path);
            return f.length();
        }
    }

    public static String getFileName(String filePath) {
        String parts[] = filePath.split("/");
        return parts[parts.length - 1];
    }


    public static void getFilePart(DataOutputStream out, String path, long skip, int size) throws IOException {
        Path filePath;
        try {
            filePath = FileSystems.getDefault().getPath(path);
        } catch (InvalidPathException e) {
            GlobalFunctions.printError("Could not find file '" + path + "'");
            return;
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             BufferedInputStream in = new BufferedInputStream(fis)) {
            if (in.skip(skip) != skip) {
                GlobalFunctions.printError("Could not skip " + skip + " bytes. aborting");
                return;
            }

            byte[] ioBuf = new byte[BUFFER_SIZE];
            int bytesToRead = size;
            int bytesCurrentlyRead;

            /**
             * if we are trying to get last part of the file it can have size less than `size`.
             * in this case `(bytesCurrentlyRead = in.read(ioBuf)) != -1` will work.
             */

            while (bytesToRead != 0 && (bytesCurrentlyRead = in.read(ioBuf)) != -1){
                out.write(ioBuf, 0, Math.min(bytesCurrentlyRead, bytesToRead));
                bytesToRead -= bytesCurrentlyRead;
            }

            out.flush();
        }
    }

    public static void writeToFile(DataInputStream in, RandomAccessFile file, long from) throws IOException {
        byte[] ioBuf = new byte[BUFFER_SIZE];
        int bytesRead;

        file.seek(from);
        while ((bytesRead = in.read(ioBuf)) != -1) {
            file.write(ioBuf, 0, bytesRead);
        }
    }
}
