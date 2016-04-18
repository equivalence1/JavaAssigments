package task.client;

import task.GlobalFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentClient {
    private static final int UPDATE_TL = 5 * 60; // 5 min
    private static final long PART_SIZE = 10 * 1024 * 1024; // 10M
    private int port;
    private TorrentClientServer clientServer;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private List<FileInfo> files;

    private Timer updateTimer = new Timer();
    private TimerTask updateTask;

    public TorrentClient(int port) {
        this.port = port;
        clientServer = new TorrentClientServer(port, files);

        updateTask = new TimerTask() {
            @Override
            public void run() {
                update();
            }
        };

        files = new LinkedList<>();
    }

    public void start() {
        if (hasSavedState())
            restoreState();
        if (clientServer != null)
            clientServer.start();

        try {
            socket = new Socket("localhost", 8081);
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            GlobalFunctions.printError("could not create socket to server. See trace.");
            e.printStackTrace();
        }

        updateTimer.schedule(updateTask, 0, UPDATE_TL / 2 * 1000); // millis
    }

    public void stop() {
        clientServer.stop();
        clientServer = null; //TODO really?
        closeSocket();
        updateTimer.cancel();
        saveState();
    }

    public ArrayList<ListResponseEntry> list() throws IOException, TimeoutException {
        out.writeByte(1);
        out.flush();

        if (!waitForResponse()) {
            throw new TimeoutException("list -- no response from server in 1 second.");
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
    public int upload(String filePath) throws IOException, NoSuchFileException, TimeoutException {
        if (!FSHandler.doesExist(filePath)) {
            throw new NoSuchFileException("file '" + filePath + "' does not exist");
        }

        long size = FSHandler.getSize(filePath);

        out.writeByte(2);
        out.writeUTF(getFileName(filePath));
        out.writeLong(size);

        if (!waitForResponse()) {
            throw new TimeoutException("upload -- no response from server in 1 second.");
        }

        int id = in.readInt();
        addFile(id, filePath, size);

        return id;
    }

    public ArrayList<SourceResponseEntry> source(int id) throws IOException, TimeoutException {
        out.writeByte(3);
        out.writeInt(id);

        if (!waitForResponse()) {
            throw new IOException("source -- no response from server in 1 second.");
        }

        ArrayList<SourceResponseEntry> ans = new ArrayList<>();

        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            byte ip[] = new byte[4];
            if (in.read(ip) != 4) {
                throw new TimeoutException("incorrect ip received from server");
            }
            short port = in.readShort();
            ans.add(new SourceResponseEntry(ip, port));
        }

        return ans;
    }

    public boolean update() {
        try {
            out.writeByte(4);
            out.writeShort(port);
            out.writeInt(files.size());

            for (FileInfo file : files) {
                out.writeInt(file.id);
            }

            if (!waitForResponse()) {
                GlobalFunctions.printWarning("update -- no response from server in 1 second.");
                return false;
            }

            return in.readBoolean();
        } catch (IOException e) {
            GlobalFunctions.printWarning("Could not sent `update` query. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    //waiting 1 sec
    private boolean waitForResponse() throws IOException {
        try {
            int time_out = 100;
            while (in.available() == 0 && time_out != 0) {
                sleep(10);
                time_out--;
            }
            return time_out != 0;
        } catch (InterruptedException e) {
            GlobalFunctions.printError("Error while waiting for response. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    private void addFile(int id, String filePath, long size) {
        FileInfo file = new FileInfo(id, filePath, size);
        for (int i = 0; i < (size + PART_SIZE - 1) / PART_SIZE; i++) {
            file.parts.add(i);
        }
    }

    private boolean hasSavedState() {
        return false;
    }

    private void restoreState() {

    }

    private void saveState() {

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
