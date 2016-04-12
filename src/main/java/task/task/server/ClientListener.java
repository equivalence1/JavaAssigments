package task.task.server;

import java.net.Socket;

/**
 * Created by equi on 08.04.16.
 *
 * @author Kravchenko Dima
 */
public class ClientListener implements Runnable{
    private Socket socket;

    public ClientListener(Socket socket) {
        this.socket = socket;
    }

    public void run() {

    }
}
