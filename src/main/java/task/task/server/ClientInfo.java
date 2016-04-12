package task.task.server;

/**
 * Created by equi on 09.04.16.
 *
 * @author Kravchenko Dima
 */
public class ClientInfo {
    public int ip;
    public int port;
    public long lastUpdateTime; // Unix time

    ClientInfo(int ip, int port) {
        this.ip    = ip;
        this.port  = port;
        lastUpdateTime = System.currentTimeMillis() / 1000L;
    }

    public void update() {
        lastUpdateTime = System.currentTimeMillis() / 1000L;
    }
}
