package task.server;

import task.GlobalFunctions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTracker {
    private static final int PORT = 8081;
    private static final String BACKUP_FILE = "backup.bak";

    ServerStates state = ServerStates.DOWN;

    private Map<Integer, FileInfo> files;
    private ReadWriteLock filesLock;

    private int curId = 0;

    private ServerSocket currentServerSocket;
    private Thread trackerThread;

    private ExecutorService clientsPool;

    public TorrentTracker() {
        TrackerRunner trackerRunner = new TrackerRunner();
        trackerThread = new Thread(trackerRunner);
    }

    public boolean start() {
        if (state == ServerStates.DOWN &&
                trackerThread != null) {
            trackerThread.start();
            return true;
        } else {
            return false;
        }
    }

    public void stop() {
        state = ServerStates.DOWN;
        trackerThread = null;
        try {
            clientsPool.shutdown();
            if (currentServerSocket != null)
                currentServerSocket.close();
        } catch (Exception e) {
            // I don't think this can ever happen
            GlobalFunctions.printWarning("Could not properly stop server. See trace");
            e.printStackTrace();
        }
    }

    private void __start() {
        if (files == null)
            files = new HashMap<>();
        if (filesLock == null)
            filesLock = new ReentrantReadWriteLock();

        if (hasSavedState())
            restoreState();

        clientsPool = Executors.newCachedThreadPool();
        state = ServerStates.RUNNING;
        startHandleConnections();
    }

    private void __stop() {
        state = ServerStates.DOWN;
        saveState();
    }

    private boolean hasSavedState() {
        File f = new File(BACKUP_FILE);
        return f.exists();
    }

    private void restoreState() {

    }

    private void saveState() {

    }

    private void startHandleConnections() {
        GlobalFunctions.printInfo("Starting to accept connections.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            currentServerSocket = serverSocket;
            while (state == ServerStates.RUNNING) {
                acceptNewConnection(serverSocket);
            }
        } catch (Exception e) {
            GlobalFunctions.printError("Could not create ServerSocket. See trace.");
            e.printStackTrace();
        }
    }

    private void acceptNewConnection(ServerSocket serverSocket) {
        try {
            Socket clientSocket = serverSocket.accept();
            GlobalFunctions.printSuccess("Connection accepted.");

            ClientListener listener = new ClientListener(clientSocket, this);
            clientsPool.submit(listener);
        } catch (Exception e) {
            if (state != ServerStates.DOWN) {
                GlobalFunctions.printError("Could not accept connection. See trace.");
                e.printStackTrace();
            }
        }
    }

    public void handleListQuery(DataOutputStream out) throws IOException {
        if (state != ServerStates.RUNNING) {
            return;
        }

        filesLock.readLock().lock();

        try {
            out.writeInt(files.size());
            for (FileInfo fileInfo : files.values()) {
                out.writeInt(fileInfo.id);
                out.writeUTF(fileInfo.name);
                out.writeLong(fileInfo.size);
            }
            out.flush();
        } finally {
            filesLock.readLock().unlock();
        }

        GlobalFunctions.printInfo("List query handled");
    }

    public void handleUploadQuery(ClientInfo clientInfo, DataInputStream in, DataOutputStream out) throws IOException {
        if (state != ServerStates.RUNNING) {
            return;
        }

        FileInfo file = new FileInfo();
        file.id = curId++;
        file.name = in.readUTF();
        file.size = in.readLong();

        clientInfo.addFile(file);
        file.addClient(clientInfo);

        filesLock.writeLock().lock();
        files.put(file.id, file);
        filesLock.writeLock().unlock();

        out.writeInt(file.id);
        out.flush();

        GlobalFunctions.printInfo("Upload query handled");
    }

    public void handleSourcesQuery(DataInputStream in, DataOutputStream out) throws IOException {
        if (state != ServerStates.RUNNING) {
            return;
        }

        int fileId = in.readInt();

        filesLock.readLock().lock();
        FileInfo fileInfo = files.get(fileId);
        filesLock.readLock().unlock();

        if (fileInfo != null) {
            try {
                fileInfo.clientsLock.readLock().lock();
                out.writeInt(fileInfo.activeClientInfos.size());
                for (ClientInfo clientInfo : fileInfo.activeClientInfos) {
                    out.write(clientInfo.socket.getInetAddress().getAddress());
                    out.writeShort(clientInfo.port);
                }
                out.flush();
            } finally {
                fileInfo.clientsLock.readLock().unlock();
            }
        } else {
            out.writeInt(0);
            out.flush();
        }

        GlobalFunctions.printInfo("Source query handled");
    }

    public void handleUpdateQuery(ClientInfo clientInfo, DataInputStream in, DataOutputStream out) throws IOException {
        if (state != ServerStates.RUNNING) {
            return;
        }

        try {
            short port = in.readShort();
            int count  = in.readInt();

            clientInfo.update();
            clientInfo.clearFiles();
            clientInfo.port = port;

            for (int i = 0; i < count; i++) {
                int id = in.readInt();

                FileInfo fileInfo = files.get(id);

                if (fileInfo != null)
                    clientInfo.addFile(fileInfo);
                fileInfo.addClient(clientInfo);
            }

            out.writeBoolean(true);
            out.flush();
        } catch (Exception e) {
            /**
             * note that if some exception occurred and we send `false`
             * which means that we did not update information, we still
             * clear files list. I think it's ok as we can not
             * guarantee which files he keeps now and it's better to
             * suppose that he doesn't have any files at all.
             */
            out.writeBoolean(false);
            out.flush();
            throw e;
        }

        GlobalFunctions.printInfo("Update query handled");
    }

    enum ServerStates {
        RUNNING, DOWN
    }

    private class TrackerRunner implements Runnable {
        public void run() {
            __start();
            __stop();
        }
    }
}
