package task.server;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 09.04.16.
 *
 * @author Kravchenko Dima
 */
class ClientInfo {
    public Socket socket; // FIXME public?
    private long lastUpdateTime; // Unix time
    private List<FileInfo> files;
    public ReadWriteLock filesLock; // FIXME public?
    int port;

    public ClientInfo(Socket socket) {
        this.socket = socket;
        files = new LinkedList<>();
        lastUpdateTime = System.currentTimeMillis() / 1000L;
        filesLock = new ReentrantReadWriteLock();
    }

    public void update() {
        lastUpdateTime = System.currentTimeMillis() / 1000L;
    }

    public long sinceLastUpdate() {
        return System.currentTimeMillis() / 1000L - lastUpdateTime;
    }

    public void addFile(FileInfo fileInfo) {
        filesLock.writeLock().lock();
        files.add(fileInfo);
        filesLock.writeLock().unlock();
    }

    public void clearFiles() {
        filesLock.writeLock().lock();
        for (FileInfo fileInfo : files) {
            fileInfo.deleteClient(this);
        }
        files.clear();
        filesLock.writeLock().unlock();
    }
}
