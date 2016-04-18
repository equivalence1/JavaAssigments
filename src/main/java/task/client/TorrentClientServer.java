package task.client;

import java.util.List;

/**
 * Created by equi on 17.04.16.
 */
public class TorrentClientServer {
    public TorrentClientServer(int port, List<FileInfo> clientFiles) {

    }

    public void start() {

    }

    public void stop() {

    }
    /*
            private static void handleGetInput(String tokens[], DataInputStream in, PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("get");
            } else {
                out.println("get " + tokens[1]);

                long size = in.readLong();
                GlobalFunctions.printSuccess("Response for 'get':");
                GlobalFunctions.printInfo("file size: " + Long.toString(size));

                byte[] ioBuf  = new byte[BUFFER_SIZE];
                for (int i = 0; i < size / BUFFER_SIZE; i++) {
                    in.read(ioBuf, 0, BUFFER_SIZE);
                    String newString = new String(ioBuf);
                    GlobalFunctions.printNormal(newString);
                }

                ioBuf = new byte[(int)(size % BUFFER_SIZE)];
                in.read(ioBuf, 0, (int)(size % BUFFER_SIZE));
                String newString = new String(ioBuf);
                GlobalFunctions.printlnNormal(newString);
            }
        }

     */
}
