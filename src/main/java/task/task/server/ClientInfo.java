package task.task.server;

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
    Socket socket;
    private long lastUpdateTime; // Unix time
    private List<FileInfo> files;
    ReadWriteLock filesLock;
    int port;

    ClientInfo(Socket socket) {
        this.socket = socket;
        files = new LinkedList<>();
        lastUpdateTime = System.currentTimeMillis() / 1000L;
        filesLock = new ReentrantReadWriteLock();
    }

    void update() {
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
        filesLock.writeLock().lock();
        for (FileInfo fileInfo : files) {
            fileInfo.deleteClient(this);
        }
        files.clear();
        filesLock.writeLock().unlock();
    }
}
