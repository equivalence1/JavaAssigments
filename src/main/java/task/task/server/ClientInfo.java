package task.task.server;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 09.04.16.
 *
 * @author Kravchenko Dima
 */
class ClientInfo {
    Socket socket;
    private long lastUpdateTime; // Unix time
    List<FileInfo> files;
    ReadWriteLock filesLock;
    int port;

    ClientInfo(Socket socket) {
        this.socket = socket;
        lastUpdateTime = System.currentTimeMillis() / 1000L;
        filesLock = new ReentrantReadWriteLock();
    }

    public void update() {
        lastUpdateTime = System.currentTimeMillis() / 1000L;
    }

    long sinceLastUpdate() {
        return System.currentTimeMillis() / 1000L - lastUpdateTime;
    }

    void addFile(FileInfo fileInfo) {
        filesLock.writeLock().lock();
        files.add(fileInfo);
        filesLock.writeLock().unlock();
    }

    void clearFiles() {
        files.clear();
    }
}
