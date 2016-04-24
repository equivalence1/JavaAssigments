package task;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class ClientInfo {

    public InetAddress address;
    public short port;

    public Set<FileInfo> files;
    private long lastUpdateTimeSec;

    /**
     * Unlike FileInfo no need of any locks here.
     * ClientInfo can only be accessed from one thread
     * (it is its client listener).
     */

    public ClientInfo(InetAddress address, short port) {
        this.address = address;
        this.port = port;

        files = new HashSet<>();
        update();
    }

    public void addFile(FileInfo file) {
        files.add(file);
    }

    public void removeFile(FileInfo file) {
        files.remove(file);
    }

    public void update() {
        lastUpdateTimeSec = System.currentTimeMillis() / 1000;
    }

    public long sinceLastUpdateSec() {
        return (System.currentTimeMillis() / 1000 - lastUpdateTimeSec);
    }

}
