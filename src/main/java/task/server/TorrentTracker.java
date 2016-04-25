package task.server;

import task.GlobalConstans;
import task.GlobalFunctions;

import java.io.*;
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
public class TorrentTracker implements TrackerServer, TrackerExecutor {

    public TrackerStatus status = TrackerStatus.STOPPED;

    private ServerSocket serverSocket;

    private ExecutorService listenersPool;
    private List<FileInfo> files;
    private ReadWriteLock filesLock;
    private AtomicInteger newFileId;

    public TorrentTracker() {
        filesLock = new ReentrantReadWriteLock();
        newFileId = new AtomicInteger(0);
        files = new ArrayList<>();
    }

    @Override
    public void start() {
        if (status != TrackerStatus.STOPPED) {
            return;
        }

        if (!restoreState()) {
            newFileId.set(0);
            files = new ArrayList<>();
        }

        listenersPool = Executors.newCachedThreadPool();
        TrackerRunner trackerRunner = new TrackerRunner();
        Thread trackerThread = new Thread(trackerRunner);
        trackerThread.start();
    }

    @Override
    public void stop() {
        if (status != TrackerStatus.RUNNING) {
            return;
        }

        status = TrackerStatus.STOPPED;
        listenersPool.shutdown();

        try {
            serverSocket.close();
        } catch (IOException e) {
            // no need to handle it.
            // we are just shutting down server.
        }

        saveState();
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

    private void saveState() {
        try {
            File backup = new File(GlobalConstans.SERVER_BACKUP_FILE);
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
             * 1. int -- newFileId value;
             * 2. int -- files.size
             * 3. files themselves.
             *    | int -- id
             *    | String -- name
             *    | long -- size
             * I don't save file's peers. It is pointless.
             */

            out.writeInt(newFileId.get());
            out.writeInt(files.size());
            for (FileInfo file : files) {
                out.writeInt(file.id);
                out.writeUTF(file.name);
                out.writeLong(file.size);
            }

            out.close();
        } catch (Exception e) {
            GlobalFunctions.printWarning("Could not save server state. See trace.");
            e.printStackTrace();

            File backup = new File(GlobalConstans.SERVER_BACKUP_FILE);
            if (backup.exists()) {
                if (!backup.delete()) {
                    GlobalFunctions.printWarning("Could not delete incomplete backup file.");
                }
            }
        }
    }

    private boolean restoreState() {
        try {
            File backup = new File(GlobalConstans.SERVER_BACKUP_FILE);
            if (!backup.exists()) {
                GlobalFunctions.printWarning("Backup file not found. Will create new clean server.");
                return false;
            }

            FileInputStream fileInputStream = new FileInputStream(backup);
            DataInputStream in = new DataInputStream(fileInputStream);

            newFileId.set(in.readInt());
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                String name = in.readUTF();
                long size = in.readLong();

                FileInfo file = new FileInfo(id, name, size);
                files.add(file);
            }

            in.close();
            return true;
        } catch (Exception e) {
            GlobalFunctions.printWarning("Could not restore state. See trace.");
            e.printStackTrace();
            return false;
        }
    }

    private class TrackerRunner implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(GlobalConstans.PORT)) {
                TorrentTracker.this.status = TrackerStatus.RUNNING;
                TorrentTracker.this.serverSocket = serverSocket; // I need to know this socket in stop()

                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientListener clientListener = new ClientListener(TorrentTracker.this, socket);
                    listenersPool.submit(clientListener);

                    GlobalFunctions.printSuccess("accepted connection from <" + socket.getInetAddress().toString() +
                            ":" + socket.getPort() + ">");
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
