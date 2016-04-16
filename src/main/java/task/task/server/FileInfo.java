package task.task.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 08.04.16.
 *
 * @author Kravchenko Dima
 */
class FileInfo {
    int id;
    String name;
    long size;

    Set<ClientInfo> activeClientInfos; //TODO should it be concurrent?
    ReadWriteLock clientsLock;

    FileInfo() {
        activeClientInfos = new HashSet<>();
        clientsLock = new ReentrantReadWriteLock();
    }

    void addClient(ClientInfo clientInfo) {
        clientsLock.writeLock().lock();
        activeClientInfos.add(clientInfo);
        clientsLock.writeLock().unlock();
    }

    void deleteClient(ClientInfo clientInfo) {
        clientsLock.writeLock().lock();
        activeClientInfos.remove(clientInfo);
        clientsLock.writeLock().unlock();
    }
}
