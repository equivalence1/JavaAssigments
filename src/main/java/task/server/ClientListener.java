package task.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import static java.lang.Thread.yield;

/**
 * Created by equi on 17.04.16.
 */
class ClientListener implements Runnable {
    private static final int UPDATE_TL = 5 * 60; // 5 min

    private static final int LIST_QUERY_CODE    = 1;
    private static final int UPLOAD_QUERY_CODE  = 2;
    private static final int SOURCE_QUERY_CODE  = 3;
    private static final int UPDATE_QUERY_CODE  = 4;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ClientInfo clientInfo;
    private byte lastQuery;
    private TorrentTracker tracker;

    public ClientListener(Socket socket, TorrentTracker tracker) {
        this.socket = socket;
        this.tracker = tracker;
        clientInfo = new ClientInfo(socket);
    }

    public void run() {
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (tracker.state == TorrentTracker.ServerStates.RUNNING) {
                if (hasPendingQuery()) {
                    handleQuery();
                } else {
                    yield();
                }
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
            case LIST_QUERY_CODE:
                handleListQuery();
                break;
            case UPLOAD_QUERY_CODE:
                handleUploadQuery();
                break;
            case SOURCE_QUERY_CODE:
                handleSourcesQuery();
                break;
            case UPDATE_QUERY_CODE:
                handleUpdateQuery();
                break;
            default:
                throw new IOException("unknown query code");
        }
    }

    private void handleListQuery() throws IOException {
        tracker.handleListQuery(out);
    }

    private void handleUploadQuery() throws IOException {
        tracker.handleUploadQuery(clientInfo, in, out);
    }

    private void handleSourcesQuery() throws IOException {
        tracker.handleSourcesQuery(in, out);
    }

    private void handleUpdateQuery() throws IOException {
        tracker.handleUpdateQuery(clientInfo, in, out);
    }

    private boolean reachedUpdateTL() {
        return clientInfo.sinceLastUpdate() > UPDATE_TL;
    }

    private void disconnect() {
        closeStreams();
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
