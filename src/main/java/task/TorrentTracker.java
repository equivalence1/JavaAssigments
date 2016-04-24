package task;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTracker implements Server, TrackerExecutor {

    /**
     * I don't need PORT to be public in this implementation.
     * But I think it can be useful if somebody wants to
     * use this server.
     *
     * The same with state. I could make it package-local
     * but I think it can be useful to get status of the
     * server.
     */
    public static final short PORT = 8081;

    public TrackerStatus status = TrackerStatus.STOPPED;

    private ServerSocket serverSocket;

    private ExecutorService listenersPool;
    private List<FileInfo> files;
    private ReadWriteLock filesLock;
    private AtomicInteger newFileId;

    public TorrentTracker() {
        listenersPool = Executors.newCachedThreadPool();
        filesLock = new ReentrantReadWriteLock();
        newFileId = new AtomicInteger(0);
        files = new ArrayList<>();
    }

    @Override
    public void start() {
        TrackerRunner trackerRunner = new TrackerRunner();
        Thread trackerThread = new Thread(trackerRunner);
        trackerThread.start();
    }

    @Override
    public void stop() {
        status = TrackerStatus.STOPPED;
        listenersPool.shutdown();

        try {
            serverSocket.close();
        } catch (IOException e) {
            // no need to handle it.
            // we are just shutting down server.
        }
    }

    @Override
    public void executeList(DataOutputStream out) throws IOException {
        try {
            filesLock.readLock().lock();
            out.writeInt(files.size());
            for (FileInfo fileInfo : files) {
                out.writeInt(fileInfo.id);
                out.writeUTF(fileInfo.name);
                out.writeLong(fileInfo.size);
            }
            out.flush();
        } finally {
            filesLock.readLock().unlock();
        }
    }

    @Override
    public void executeUpload(ClientInfo peer, String name, long size, DataOutputStream out) throws IOException {
        int id = newFileId.getAndAdd(1);

        FileInfo fileInfo = new FileInfo(id, name, size);
        fileInfo.addPeer(peer);
        peer.addFile(fileInfo);

        filesLock.writeLock().lock();
        files.add(id, fileInfo);
        filesLock.writeLock().unlock();

        out.writeInt(id);
        out.flush();
    }

    @Override
    public void executeSources(int id, DataOutputStream out) throws IOException {
        FileInfo file = files.get(id);

        try {
            file.peersLock.readLock().lock();
            out.writeInt(file.peers.size());
            for (ClientInfo peer : file.peers) {
                out.write(peer.address.getAddress());
                out.writeShort(peer.port);
            }
        } finally {
            file.peersLock.readLock().unlock();
        }

        out.flush();
    }

    @Override
    public void executeUpdate(ClientInfo clientInfo, List<Integer> ids, DataOutputStream out) throws IOException {
        try {
            for (FileInfo file : clientInfo.files) {
                file.removePeer(clientInfo);
            }
            clientInfo.files.clear();

            for (int id : ids) {
                if (id >= files.size()) {
                    out.writeBoolean(false);
                    out.flush();
                    return;
                }

                FileInfo file = files.get(id);
                file.addPeer(clientInfo);
                clientInfo.files.add(file);
            }
        } catch (Exception e) {
            out.writeBoolean(false);
            out.flush();
            throw e;
        }

        out.writeBoolean(true);
        out.flush();
    }

    public enum TrackerStatus {
        RUNNING, STOPPED
    }

    private class TrackerRunner implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                TorrentTracker.this.status = TrackerStatus.RUNNING;
                TorrentTracker.this.serverSocket = serverSocket; // I need to know this socket in stop()

                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientListener clientListener = new ClientListener(TorrentTracker.this, socket);
                    listenersPool.submit(clientListener);
                }
            } catch (IOException e) {
                if (status == TrackerStatus.RUNNING) {
                    GlobalFunctions.printError("Error occurred. See trace.");
                    e.printStackTrace();
                }
            }
        }
    }

}
