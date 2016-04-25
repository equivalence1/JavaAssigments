package task.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class FileInfo {

    public int id;
    public String name;
    public long size;

    public Set<ClientInfo> peers;
    public ReadWriteLock peersLock;

    public FileInfo(int id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;

        peers = new HashSet<>();
        peersLock = new ReentrantReadWriteLock();
    }

    public void addPeer(ClientInfo peer) {
        peersLock.writeLock().lock();
        peers.add(peer);
        peersLock.writeLock().unlock();
    }

    public void removePeer(ClientInfo peer) {
        peersLock.writeLock().lock();
        peers.remove(peer);
        peersLock.writeLock().unlock();
    }

}
