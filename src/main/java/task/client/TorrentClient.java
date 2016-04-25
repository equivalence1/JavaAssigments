package task.client;

import task.GlobalConstans;
import task.GlobalFunctions;

import java.io.*;
import java.net.Socket;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentClient {
    public ClientStates status = ClientStates.DOWN;
    /**
     * only ClientServer should have access to this fields
     * so I make them package-local
     */
    int port;
    Map<Integer, FileInfo> files;
    ReadWriteLock filesLock;

    private TorrentClientServer clientServer;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private Timer updateTimer;
    private TimerTask updateTask;

    public TorrentClient(int port) {
        this.port = port;

        updateTask = new TimerTask() {
            @Override
            public void run() {
                update();
            }
        };
        files = new HashMap<>();
        filesLock = new ReentrantReadWriteLock();
        clientServer = new TorrentClientServer(this);
    }

    public void start() {
        if (status == ClientStates.RUNNING) {
            return;
        }

        if (!restoreState()) {
            GlobalFunctions.printWarning("Backup not found. Will create new clean client.");
            files = new HashMap<>();
        } else {
            GlobalFunctions.printSuccess("Client was successfully restored from backup.");
        }

        try {
            socket = new Socket("localhost", GlobalConstans.PORT);
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            GlobalFunctions.printError("Could not create socket to server. See trace.");
            e.printStackTrace();
        }

        clientServer.start();

        /**
         * Updating every 2.5 minutes with required 5 seems ok
         */
        updateTimer = new Timer();
        updateTimer.schedule(updateTask, 0, GlobalConstans.UPDATE_TL / 2 * 1000); // millis
    }

    public void stop() {
        if (status == ClientStates.DOWN) {
            return;
        }
        status = ClientStates.DOWN;

        clientServer.stop();
        updateTimer.cancel();
        closeSocket();
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
    public int upload(String filePath) throws IOException, TimeoutException {
        if (!FSHandler.doesExist(filePath)) {
            throw new NoSuchFileException("file '" + filePath + "' does not exist");
        }

        long size = FSHandler.getSize(filePath);

        out.writeByte(2);
        out.writeUTF(FSHandler.getFileName(filePath));
        out.writeLong(size);
        out.flush();

        if (!waitForResponse()) {
            throw new TimeoutException("upload -- no response from server in 1 second.");
        }

        int id = in.readInt();
        addFile(id, filePath, size);

        return id;
    }

    public ArrayList<SourcesResponseEntry> sources(int id) throws IOException, TimeoutException {
        out.writeByte(3);
        out.writeInt(id);
        out.flush();

        if (!waitForResponse()) {
            throw new TimeoutException("source -- no response from server in 1 second.");
        }

        ArrayList<SourcesResponseEntry> ans = new ArrayList<>();

        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            byte ip[] = new byte[4];
            if (in.read(ip) != 4) {
                throw new IOException("incorrect ip received from server");
            }
            short port = in.readShort();
            ans.add(new SourcesResponseEntry(ip, port));
        }

        return ans;
    }

    public boolean update() {
        try {
            out.writeByte(4);
            out.writeShort(port);
            out.writeInt(files.size());

            for (FileInfo file : files.values()) {
                out.writeInt(file.id);
            }
            out.flush();

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

    public ArrayList<Integer> stat(String ip, short port, int id) throws IOException, TimeoutException {
        try (
                Socket socket = new Socket(ip, port);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            out.writeByte(1);
            out.writeInt(id);
            out.flush();

            if (!waitForResponse(in)) {
                throw new TimeoutException("stat -- no response from peer in 1 second.");
            }

            ArrayList<Integer> res = new ArrayList<>();

            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                res.add(in.readInt());
            }

            return res;
        }
    }

    public void get(String ip, Short port, int id, int part) throws IOException, TimeoutException {
        try (
                Socket socket = new Socket(ip, port);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            out.writeByte(2);
            out.writeInt(id);
            out.writeInt(part);
            out.flush();

            if (!waitForResponse(in)) {
                throw new TimeoutException("get -- no response from peer in 1 second.");
            }

            String fileName = "file_" + id + ".tor";
            long fileSize;
            boolean inFiles = false;

            filesLock.readLock().lock();
            if (files.containsKey(id)) {
                fileSize = files.get(id).size;
                inFiles = true;
            } else {
                fileSize = getFileSize(id);
            }
            filesLock.readLock().unlock();

            File file = new File(fileName);
            RandomAccessFile rafile = new RandomAccessFile(file, "rw");
            if (!inFiles) {
                rafile.setLength(fileSize);
            }

            FSHandler.writeToFile(in, rafile, part * GlobalConstans.PART_SIZE);

            filesLock.writeLock().lock();
            if (!files.containsKey(id)) {
                FileInfo fileInfo = new FileInfo(id, fileName, file.getAbsolutePath(), fileSize);
                fileInfo.parts.add(part);
                files.put(id, fileInfo);
            } else {
                files.get(id).parts.add(part);
            }
            filesLock.writeLock().unlock();

            GlobalFunctions.printNormal("part written to " + file.getName());
            update();
        }
    }

    private long getFileSize(int id) throws IOException, TimeoutException {
        ArrayList<ListResponseEntry> list = list();

        return list.get(id).size;
    }

    //waiting 1 sec
    private boolean waitForResponse(DataInputStream in) throws IOException {
        try {
            int time_out = 100;
            while (in.available() == 0 && time_out != 0) {
                Thread.sleep(10);
                time_out--;
            }
            return time_out != 0;
        } catch (InterruptedException e) {
            GlobalFunctions.printError("Error while waiting for response. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    //waiting 1 sec
    private boolean waitForResponse() throws IOException {
        return waitForResponse(in);
    }

    private void addFile(int id, String filePath, long size) {
        FileInfo file = new FileInfo(id, FSHandler.getFileName(filePath), filePath, size);
        for (int i = 0; i < (size + GlobalConstans.PART_SIZE - 1) / GlobalConstans.PART_SIZE; i++) {
            file.parts.add(i);
        }
        GlobalFunctions.printSuccess("added " + id + " " + file.parts.size());

        filesLock.writeLock().lock();
        files.put(id, file);
        filesLock.writeLock().unlock();
    }

    private boolean restoreState() {
        try {
            File backup = new File(GlobalConstans.CLIENT_BACKUP_FILE);
            if (!backup.exists()) {
                GlobalFunctions.printWarning("Backup file not found. Will create new clean client.");
                return false;
            }

            FileInputStream fileInputStream = new FileInputStream(backup);
            DataInputStream in = new DataInputStream(fileInputStream);

            port = in.readInt();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                String name = in.readUTF();
                String path = in.readUTF();
                long size = in.readLong();

                FileInfo file = new FileInfo(id, name, path, size);
                int partsCount = in.readInt();
                for (int j = 0; j < partsCount; j++) {
                    int part = in.readInt();
                    file.parts.add(part);
                }

                files.put(id, file);
            }

            in.close();
            return true;
        } catch (Exception e) {
            GlobalFunctions.printWarning("Could not restore state. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    private void saveState() {
        try {
            File backup = new File(GlobalConstans.CLIENT_BACKUP_FILE);
            if (backup.exists()) {
                if (!backup.delete()) {
                    throw new IOException("could not delete old backup");
                }
            }
            if (!backup.createNewFile()) {
                throw new IOException("could not create backup file");
            }

            FileOutputStream fileOutputStream = new FileOutputStream(backup);
            DataOutputStream out = new DataOutputStream(fileOutputStream);

            /**
             * Buckup file structure
             * 1. int -- port
             * 2. int -- files.size
             * 3. files themselves.
             *    | int -- id
             *    | String -- name
             *    | String -- path
             *    | long -- size
             *    | int -- parts count
             *    | <int>* -- parts numbers
             */

            /**
             * no need for locks here.
             */

            out.writeInt(port);
            out.writeInt(files.size());
            for (FileInfo file : files.values()) {
                out.writeInt(file.id);
                out.writeUTF(file.name);
                out.writeUTF(file.path);
                out.writeLong(file.size);
                out.writeInt(file.parts.size());
                for (int part : file.parts) {
                    out.writeInt(part);
                }
            }

            out.close();

            GlobalFunctions.printSuccess("Client backup successfully saved.");
        } catch (Exception e) {
            GlobalFunctions.printWarning("Could not save client state. See trace.");
            e.printStackTrace();

            File backup = new File(GlobalConstans.CLIENT_BACKUP_FILE);
            if (backup.exists()) {
                if (!backup.delete()) {
                    GlobalFunctions.printWarning("Could not delete incomplete backup file.");
                }
            }
        }
    }

    private void closeSocket() {
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum ClientStates {
        RUNNING, DOWN
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

    public static class SourcesResponseEntry {
        public byte ip[];
        public short port;

        public SourcesResponseEntry(byte ip[], short port) {
            this.ip   = ip;
            this.port = port;
        }
    }

}
