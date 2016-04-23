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

    /**
     * Unlike FileInfo no need of any locks here.
     * ClientInfo can only be accessed from one thread
     * (it is its client listener).
     */

    ClientInfo(InetAddress address, short port) {
        this.address = address;
        this.port = port;

        files = new HashSet<>();
    }

    void addFile(FileInfo file) {
        files.add(file);
    }

    void removeFile(FileInfo file) {
        files.remove(file);
    }

}
