package task.task.server;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 08.04.16.
 *
 * @author Kravchenko Dima
 */
public class FileInfo {
    int id;
    String name;
    long size;

    List<ClientInfo> activeClientInfos; //TODO should it be concurrent?
    ReadWriteLock clientsLock;

    public FileInfo() {
        activeClientInfos = new LinkedList<>();
        clientsLock = new ReentrantReadWriteLock();
    }

    void addClient(ClientInfo clientInfo) {
        clientsLock.writeLock().lock();
        activeClientInfos.add(clientInfo);
        clientsLock.writeLock().unlock();
    }
}
