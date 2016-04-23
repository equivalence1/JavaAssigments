package task.client;

import task.GlobalFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by equi on 17.04.16.
 */
public class TorrentClientServer {
    public ServerStates state = ServerStates.DOWN;
    private static final long PART_SIZE = 10 * 1024 * 1024; // 10M

    private int port;
    private Map<Integer, FileInfo> clientFiles;
    private Thread serverThread;
    private ExecutorService peersPool;
    private ServerSocket currentServerSocket;

    public TorrentClientServer(int port, Map<Integer, FileInfo> clientFiles) {
        this.port = port;
        this.clientFiles = clientFiles;
        serverThread = new Thread(new ServerRunner());
    }

    public void start() {
        if (state == TorrentClientServer.ServerStates.DOWN &&
                serverThread != null) {
            serverThread.start();
        }
    }

    public void stop() {
        serverThread = null;
        try {
            peersPool.shutdown();
            if (currentServerSocket != null)
                currentServerSocket.close();
        } catch (Exception e) {
            // I don't think this can ever happen
            GlobalFunctions.printWarning("Could not properly stop server. See trace");
            e.printStackTrace();
        }
    }

    public void handleStatQuery(DataInputStream in, DataOutputStream out) throws IOException {
        GlobalFunctions.printInfo("handling stat query");
        int id = in.readInt();
        FileInfo clientFile = clientFiles.get(id);

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

        FileInfo fileInfo = clientFiles.get(id);
        if (fileInfo == null || !fileInfo.parts.contains(part)) {
            return;
        }

        long pos = part * PART_SIZE;

        FSHandler.getFilePart(out, fileInfo.path, pos, PART_SIZE);
        out.flush();
    }

    private void __start() {
        peersPool = Executors.newCachedThreadPool();
        state = ServerStates.RUNNING;
        startHandleConnections();
    }

    private void __stop() {
        peersPool.shutdown();
    }

    private void startHandleConnections() {
        GlobalFunctions.printInfo("Starting to accept connections.");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            currentServerSocket = serverSocket;
            while (state == ServerStates.RUNNING) {
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
            GlobalFunctions.printSuccess("Connection accepted."); // TODO from whom?

            PeerListener listener = new PeerListener(clientSocket, this);
            peersPool.submit(listener);
        } catch (Exception e) {
            if (state != ServerStates.DOWN) {
                GlobalFunctions.printError("Could not accept connection. See trace.");
                e.printStackTrace();
            }
        }
    }

    public enum ServerStates {
        RUNNING, DOWN
    }

    private class ServerRunner implements Runnable {
        public void run() {
            __start();
            __stop();
        }
    }
}
