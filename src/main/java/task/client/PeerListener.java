package task.client;

import task.GlobalFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */
public class PeerListener implements Runnable {
    private static final int STAT_QUERY_CODE  = 1;
    private static final int GET_QUERY_CODE   = 2;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private byte lastQuery;
    private TorrentClientServer server;

    public PeerListener(Socket socket, TorrentClientServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            if (hasPendingQuery()) {
                handleQuery();
            }
        } catch (Exception e) {
            GlobalFunctions.printError("Exception occurred. See trace.");
            e.printStackTrace();
        } finally {
            closeSocket();
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
            case STAT_QUERY_CODE:
                handleStatQuery();
                break;
            case GET_QUERY_CODE:
                handleGetQuery();
                break;
            default:
                throw new IOException("unknown query code");
        }
    }

    private void handleStatQuery() throws IOException {
        server.handleStatQuery(in, out);
    }

    private void handleGetQuery() throws IOException {
        server.handleGetQuery(in, out);
    }

    private void closeSocket() {
        GlobalFunctions.printInfo("closing listener");
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

