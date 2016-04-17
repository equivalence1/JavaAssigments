package task.client;

import task.GlobalFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentClient {
    private int port;
    private TorrentClientServer clientServer;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    public TorrentClient(int port) {
        this.port = port;
        clientServer = new TorrentClientServer(port);

        try {
            socket = new Socket("localhost", 8081);
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            GlobalFunctions.printError("could not create socket to server. See trace.");
            e.printStackTrace();
        }
    }

    public void start() {
        if (clientServer != null)
            clientServer.start();
        //TODO scheduled update.
    }

    public void stop() {
        clientServer.stop();
        clientServer = null;
        closeSocket();
    }

    public ArrayList<ListResponseEntry> list() throws IOException { //TODO return this list
        out.writeByte(1);
        out.flush();

        if (!waitForResponse()) {
            return null;
        }

        ArrayList<ListResponseEntry> ans = new ArrayList<>();

        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id      = in.readInt();
            String name = in.readUTF();
            long size   = in.readLong();

            ans.add(new ListResponseEntry(id, name, size));
        }

        return ans;
    }

    // returns -1 if error occurs
    public int upload(String filePath) throws IOException {
        out.writeByte(2);
        out.writeUTF(getFileName(filePath));

        if (!waitForResponse()) {
            return -1;
        }

        return in.readInt();
    }

    public ArrayList<SourceResponseEntry> source(int id) throws IOException { //TODO return this list
        out.writeByte(3);
        out.writeInt(id);

        if (!waitForResponse()) {
            return null;
        }

        ArrayList<SourceResponseEntry> ans = new ArrayList<>();

        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            byte ip[] = new byte[4];
            if (in.read(ip) != 4) {
                throw new IOException("incorrect ip received from server");
            }
            short port = in.readShort();
            ans.add(new SourceResponseEntry(ip, port));
        }

        return ans;
    }

    private void update() throws IOException {
        out.writeByte(4);

    }

    private boolean waitForResponse() throws IOException {
        try {
            int time_out = 1000;
            while (in.available() == 0 && time_out != 0) {
                sleep(1);
                time_out--;
            }
            if (time_out == 0) {
                GlobalFunctions.printWarning("Server did not respond properly in 1 second.");
                GlobalFunctions.printWarning("It's not safe to use it anymore.");
                return false;
            } else {
                return true;
            }
        } catch (InterruptedException e) {
            GlobalFunctions.printError("Error while waiting for response. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    private void printListEntry(int id, String name, long size) {
        GlobalFunctions.printlnNormal("file id: " + id);
        GlobalFunctions.printlnNormal("file name: " + name);
        GlobalFunctions.printlnNormal("file size: " + size);
        GlobalFunctions.printlnNormal("");
    }

    private String getFileName(String filePath) {
        String parts[] = filePath.split("/");
        return parts[parts.length - 1];
    }

    private void closeSocket() {
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ListResponseEntry {
        public int id;
        public String name;
        public long size;

        public ListResponseEntry(int id, String name, long size) {
            this.id   = id;
            this.name = name;
            this.size = size;
        }
    }

    public static class SourceResponseEntry {
        public byte ip[];
        public short port;

        public SourceResponseEntry(byte ip[], short port) {
            this.ip   = ip;
            this.port = port;
        }
    }
}
