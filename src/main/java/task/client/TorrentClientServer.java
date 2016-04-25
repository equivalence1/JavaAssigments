package task.client;

import task.GlobalConstans;
import task.GlobalFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */

public class TorrentClientServer {
    private Thread serverThread;
    private ExecutorService peersPool;
    private ServerSocket currentServerSocket;

    private TorrentClient torrentClient;

    public TorrentClientServer(TorrentClient torrentClient) {
        this.torrentClient = torrentClient;
    }

    public void start() {
        serverThread = new Thread(new ServerRunner());
        serverThread.start();
    }

    public void stop() {
        try {
            peersPool.shutdown();
            currentServerSocket.close();
        } catch (Exception e) {
            // I don't think this can ever happen
            GlobalFunctions.printWarning("Could not properly stop server. See trace");
            e.printStackTrace();
        }
    }

    public void handleStatQuery(DataInputStream in, DataOutputStream out) throws IOException {
        int id = in.readInt();

        torrentClient.filesLock.readLock().lock();
        FileInfo clientFile = torrentClient.files.get(id);
        torrentClient.filesLock.readLock().unlock();

        if (clientFile == null) {
            out.writeInt(0);
        } else {
            out.writeInt(clientFile.parts.size());
            for (int part : clientFile.parts) {
                out.writeInt(part);
            }
        }
        out.flush();
    }

    public void handleGetQuery(DataInputStream in, DataOutputStream out) throws IOException {
        int id = in.readInt();
        int part = in.readInt();

        torrentClient.filesLock.readLock().lock();
        FileInfo fileInfo = torrentClient.files.get(id);
        torrentClient.filesLock.readLock().unlock();

        if (fileInfo == null || !fileInfo.parts.contains(part)) {
            return;
        }

        long pos = part * GlobalConstans.PART_SIZE;

        FSHandler.getFilePart(out, fileInfo.path, pos, (int)GlobalConstans.PART_SIZE);
        out.flush();
    }

    private void __start() {
        peersPool = Executors.newCachedThreadPool();
        torrentClient.status = TorrentClient.ClientStates.RUNNING;
        startHandleConnections();
    }

    private void __stop() {
        peersPool.shutdown();
    }

    private void startHandleConnections() {
        try (ServerSocket serverSocket = new ServerSocket(torrentClient.port)) {
            GlobalFunctions.printInfo("Starting to accept connections. Current port is " + torrentClient.port);
            currentServerSocket = serverSocket;
            while (torrentClient.status == TorrentClient.ClientStates.RUNNING) {
                acceptNewConnection(serverSocket);
            }
        } catch (Exception e) {
            GlobalFunctions.printError("Could not create ServerSocket. See trace.");
            e.printStackTrace();
        }
    }

    private void acceptNewConnection(ServerSocket serverSocket) {
        try {
            Socket clientSocket = serverSocket.accept();
            GlobalFunctions.printSuccess("Connection accepted from <" +
                    clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort() + ">");

            PeerListener listener = new PeerListener(clientSocket, this);
            peersPool.submit(listener);
        } catch (Exception e) {
            if (torrentClient.status != TorrentClient.ClientStates.DOWN) {
                GlobalFunctions.printError("Could not accept connection. See trace.");
                e.printStackTrace();
            }
        }
    }

    private class ServerRunner implements Runnable {
        public void run() {
            __start();
            __stop();
        }
    }
}
