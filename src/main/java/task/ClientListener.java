package task;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class ClientListener implements Runnable {

    private static final int UPDATE_TL = 5 * 60; // 5 min;

    private static final byte LIST_QUERY_CODE     = 1;
    private static final byte UPLOAD_QUERY_CODE   = 2;
    private static final byte SOURCES_QUERY_CODE  = 3;
    private static final byte UPDATE_QUERY_CODE   = 4;

    private TorrentTracker tracker;

    private ClientInfo client;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    public ClientListener(TorrentTracker tracker, Socket socket) {
        /**
         * we will know client's PORT only after update.
         * So for now I just set it to 0.
         */
        this.tracker = tracker;
        this.socket = socket;
        client = new ClientInfo(socket.getInetAddress(), (short)0);
    }

    public void run() {
        try {
            client.update();
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (client.sinceLastUpdateSec() <= UPDATE_TL &&
                    tracker.status == TorrentTracker.TrackerStatus.RUNNING) {
                if (hasPendingQuery()) {
                    handleQuery();
                } else {
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            printErrorMessage();
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private boolean hasPendingQuery() throws IOException {
        return in.available() > 0;
    }

    private void handleQuery() throws IOException {
        byte queryCode = in.readByte();
        switch (queryCode) {
            case LIST_QUERY_CODE:
                handleListQuery();
                break;
            case UPLOAD_QUERY_CODE:
                handleUploadQuery();
                break;
            case SOURCES_QUERY_CODE:
                handleSourcesQuery();
                break;
            case UPDATE_QUERY_CODE:
                handleUpdateQuery();
                break;
        }
    }

    private void handleListQuery() throws IOException {
        tracker.executeList(out);
    }

    private void handleUploadQuery() throws IOException {
        String name = in.readUTF();
        long size = in.readLong();

        tracker.executeUpload(client, name, size, out);
    }

    private void handleSourcesQuery() throws IOException {
        int id = in.readInt();

        tracker.executeSources(id, out);
    }

    private void handleUpdateQuery() throws IOException {
        short port = in.readShort();
        int count = in.readInt();

        List<Integer> fileIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            fileIds.add(in.readInt());
        }

        client.port = port;

        tracker.executeUpdate(client, fileIds, out);
    }

    private void disconnect() {
        GlobalFunctions.printInfo("Client <" + socket.getInetAddress().toString() +
                ":" + socket.getPort() + "> disconnected.");
        closeSocket();
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printErrorMessage() {
        String err = "Error occurred while dealing with client <" + socket.getInetAddress().toString() + ":" +
                socket.getPort() + ">. See trace.";
        GlobalFunctions.printError(err);
    }
}
