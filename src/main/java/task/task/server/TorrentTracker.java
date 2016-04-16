package task.task.server;

import task.GlobalFunctions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTracker {
    private static final int PORT = 8081;
    private static final int UPDATE_TL = 5 * 60; // 5 min
    private ServerStates state;

    private Map<Integer, FileInfo> files;
    private ReadWriteLock filesLock;

    private int curId = 0;

    public void start() {
        state = ServerStates.RUNNING;

        files = new HashMap<>();
        filesLock = new ReentrantReadWriteLock();

        startHandleConnections();
    }

    private void startHandleConnections() {
        GlobalFunctions.printInfo("Starting to accept connections.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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
            GlobalFunctions.printSuccess("Connection accepted."); // TODO from who?

            ClientListener listener = new ClientListener(clientSocket);
            Thread clientThread = new Thread(listener);
            clientThread.start();
        } catch (Exception e) {
            GlobalFunctions.printError("Could not accept connection. See trace.");
            e.printStackTrace();
        }
    }

    private enum ServerStates {
        RUNNING, DOWN
    }

    private class ClientListener implements Runnable{
        private Socket socket;

        private DataInputStream in;
        private DataOutputStream out;

        private ClientInfo clientInfo;

        private byte lastQuery;

        private ClientListener(Socket socket) {
            this.socket = socket;
            clientInfo = new ClientInfo(this.socket);
        }

        public void run() {
            try {
                in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    if (hasPendingQuery())
                        handleQuery();
                    if (reachedUpdateTL())
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private boolean hasPendingQuery() throws IOException {
            try {
                lastQuery = in.readByte();
                return true;
            } catch (EOFException e) {
                return false;
            }
        }

        private void handleQuery() throws IOException {
            switch (lastQuery) {
                case 1:
                    handleListQuery();
                    break;
                case 2:
                    handleUploadQuery();
                    break;
                case 3:
                    handleSourcesQuery();
                    break;
                case 4:
                    handleUpdateQuery();
                    break;
                default:
                    throw new IOException("unknown query code");
            }
        }

        private void handleListQuery() throws IOException {
            filesLock.readLock().lock();

            try {
                out.writeInt(files.size());
                for (FileInfo fileInfo : files.values()) {
                    out.writeInt(fileInfo.id);
                    out.writeUTF(fileInfo.name);
                    out.writeLong(fileInfo.size);
                }
            } finally {
                filesLock.readLock().unlock();
            }
        }

        private void handleUploadQuery() throws IOException {
            FileInfo file = new FileInfo();
            file.id = curId++;
            file.name = in.readUTF();
            file.size = in.readLong();

            clientInfo.addFile(file);
            file.addClient(clientInfo);

            filesLock.writeLock().lock();
            files.put(file.id, file);
            filesLock.writeLock().unlock();
        }

        private void handleSourcesQuery() throws IOException {
            int fileId = in.readInt();

            filesLock.readLock().lock();
            FileInfo fileInfo = files.get(fileId);
            filesLock.readLock().unlock();

            if (fileInfo != null) {
                try {
                    fileInfo.clientsLock.readLock().lock();
                    out.writeInt(fileInfo.activeClientInfos.size());
                    for (ClientInfo clientInfo : fileInfo.activeClientInfos) {
                        out.write(clientInfo.socket.getInetAddress().getAddress());
                        out.writeShort(clientInfo.socket.getPort());
                    }
                } finally {
                    fileInfo.clientsLock.readLock().unlock();
                }
            }
        }

        private void handleUpdateQuery() throws IOException {
            try {
                short port = in.readShort();
                int count  = in.readInt();

                clientInfo.update();
                clientInfo.clearFiles();
                clientInfo.filesLock.writeLock().lock();
                clientInfo.port = port;

                for (int i = 0; i < count; i++) {
                    int id = in.readInt();

                    FileInfo fileInfo = files.get(id);

                    if (fileInfo != null)
                        clientInfo.addFile(fileInfo);
                }

                out.writeBoolean(true);
            } catch (Exception e) {
                /**
                 * note that if some exception occurred and we send `false`
                 * which means that we did not update information, we still
                 * clear files list. I think it's ok as we can not
                 * guarantee which files he keeps now and it's better to
                 * suppose that he doesn't have any files at all.
                 */
                out.writeBoolean(false);
                throw e;
            } finally {
                clientInfo.filesLock.writeLock().unlock();
            }
        }

        private boolean reachedUpdateTL() {
            return clientInfo.sinceLastUpdate() > UPDATE_TL;
        }

        private void disconnect() {
            closeStreams();
            state = ServerStates.DOWN;
        }

        private void closeStreams() {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
