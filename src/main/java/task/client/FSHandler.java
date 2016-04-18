package task.client;

import java.io.*;
import java.nio.file.*;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */

//means File System Handler
public class FSHandler {
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


    public static void getFilePart(DataOutputStream out, String path, long skip, long size) throws IOException {
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
            in.skip(skip);

            byte[] ioBuf = new byte[BUFFER_SIZE];
            long bytesToRead = size;
            int bytesRead;

            while (bytesToRead != 0 && (bytesRead = in.read(ioBuf)) != -1){
                out.write(ioBuf, 0, Math.min(bytesRead, bytesRead));
                bytesToRead -= bytesRead;
            }
        }
    }

    public static void writeToFile(DataInputStream in, RandomAccessFile file, long from) throws IOException {
        byte[] ioBuf = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = in.read(ioBuf)) != -1) {
            file.write(ioBuf, (int)from, bytesRead);
            from += bytesRead;
        }
    }
}
