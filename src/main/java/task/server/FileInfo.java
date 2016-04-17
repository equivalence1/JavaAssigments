package task.server;

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

    public FileInfo() {
        activeClientInfos = new HashSet<>();
        clientsLock = new ReentrantReadWriteLock();
    }

    public void addClient(ClientInfo clientInfo) {
        clientsLock.writeLock().lock();
        activeClientInfos.add(clientInfo);
        clientsLock.writeLock().unlock();
    }

    public void deleteClient(ClientInfo clientInfo) {
        clientsLock.writeLock().lock();
        activeClientInfos.remove(clientInfo);
        clientsLock.writeLock().unlock();
    }
}
