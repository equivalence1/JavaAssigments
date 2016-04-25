package task.server;

import com.google.common.collect.Lists;
import org.junit.Test;
import task.GlobalConstans;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTrackerTest {

    private static final String SERVER_HOST = "localhost";
    private static final short SERVER_PORT = 8081;

    @Test
    public void testStartAndStop() throws Exception {
        TorrentTracker tracker = new TorrentTracker();
        assertEquals(TorrentTracker.TrackerStatus.STOPPED, tracker.status);
        tracker.start();
        Thread.sleep(100);
        assertEquals(TorrentTracker.TrackerStatus.RUNNING, tracker.status);
        tracker.stop();
        assertEquals(TorrentTracker.TrackerStatus.STOPPED, tracker.status);
        deleteBackup();
    }

    @Test
    public void testExecuteList() throws Exception {
        TorrentTracker tracker = new TorrentTracker();

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        ArrayList<FileInfo> files = new ArrayList<>();

        final int n = 5;

        ClientInfo client = new ClientInfo(InetAddress.getLocalHost(), (short)1);
        for (int i = 0; i < n; i++) {
            FileInfo file = new FileInfo(i, "file" + i, 100 * i);
            files.add(file);
            tracker.executeUpload(client, file.name, file.size, out);
        }

        tracker.executeList(out);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream in = new DataInputStream(byteIn);

        for (int i = 0; i < n; i++) {
            assertEquals(i, in.readInt()); // ("file" + i) id
        }

        assertEquals(n, in.readInt()); // count
        for (int i = 0; i < 5; i++) {
            int id = in.readInt(); // file id
            assertEquals(files.get(id).name, in.readUTF()); // file name
            assertEquals(files.get(id).size, in.readLong()); // file size
        }
    }

    @Test
    public void testExecuteUpload() throws Exception {
        TorrentTracker tracker = new TorrentTracker();

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        ClientInfo client1 = new ClientInfo(InetAddress.getLocalHost(), (short)1);
        tracker.executeUpload(client1, "file1", 100, out);
        tracker.executeUpload(client1, "file2", 200, out);

        ClientInfo client2 = new ClientInfo(InetAddress.getLocalHost(), (short)2);
        tracker.executeUpload(client2, "file1", 100, out);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream in = new DataInputStream(byteIn);

        assertEquals(0, in.readInt()); // file1 id
        assertEquals(1, in.readInt()); // file2 id
        assertEquals(2, in.readInt()); // file1 id
    }

    @Test
    public void testExecuteSources() throws Exception {
        TorrentTracker tracker = new TorrentTracker();

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        ClientInfo client1 = new ClientInfo(InetAddress.getLocalHost(), (short)1);
        ClientInfo client2 = new ClientInfo(InetAddress.getLocalHost(), (short)2);
        ClientInfo client3 = new ClientInfo(InetAddress.getLocalHost(), (short)3);

        tracker.executeUpload(client1, "file", 100, out);
        tracker.executeUpdate(client2, Lists.newArrayList(0), out);
        tracker.executeUpdate(client3, Lists.newArrayList(0), out);
        tracker.executeUpdate(client1, Lists.newArrayList(),  out);

        byteOut.reset();

        tracker.executeSources(0, out); // should be client2 + client3

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream in = new DataInputStream(byteIn);

        byte realIp[] = InetAddress.getLocalHost().getAddress();
        byte ip[] = new byte[4];
        boolean isPeer[] = new boolean[4];

        assertEquals(2, in.readInt());
        for (int i = 0; i < 2; i++) {
            assertEquals(4, in.read(ip));
            assertArrayEquals(realIp, ip);
            isPeer[in.readShort()] = true;
        }

        assertEquals(false, isPeer[1]);
        assertEquals(true, isPeer[2]);
        assertEquals(true, isPeer[3]);
    }

    @Test
    public void testExecuteUpdate() throws Exception {
        TorrentTracker tracker = new TorrentTracker();

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        ClientInfo client = new ClientInfo(InetAddress.getLocalHost(), (short)1);
        tracker.executeUpdate(client, Lists.newArrayList(1, 2, 3), out);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream in = new DataInputStream(byteIn);

        assertEquals(false, in.readBoolean());

        final int n = 5;

        for (int i = 0; i < n; i++) {
            tracker.executeUpload(client, "name", 100, out);
        }

        byteOut.reset();

        tracker.executeUpdate(client, Lists.newArrayList(1, 2, 3), out);

        byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        in = new DataInputStream(byteIn);

        assertEquals(true, in.readBoolean());
    }

    @Test
    public void testStateRestoring() throws Exception {
        deleteBackup();

        TorrentTracker tracker1 = new TorrentTracker();
        tracker1.start();
        Thread.sleep(100);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        ClientInfo client = new ClientInfo(InetAddress.getLocalHost(), (short)1);

        FileInfo file1 = new FileInfo(0, "name1", 100);
        tracker1.executeUpload(client, file1.name, file1.size, out);

        FileInfo file2 = new FileInfo(1, "name2", 200);
        tracker1.executeUpload(client, file2.name, file2.size, out);

        tracker1.stop();
        Thread.sleep(100);

        TorrentTracker tracker2 = new TorrentTracker();
        tracker2.start();
        Thread.sleep(100);

        byteOut.reset();

        tracker2.executeList(out);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream in = new DataInputStream(byteIn);

        // count check
        assertEquals(2, in.readInt());

        // file1 check
        assertEquals(file1.id, in.readInt());
        assertEquals(file1.name, in.readUTF());
        assertEquals(file1.size, in.readLong());

        // file2 check
        assertEquals(file2.id, in.readInt());
        assertEquals(file2.name, in.readUTF());
        assertEquals(file2.size, in.readLong());

        tracker2.stop();
        deleteBackup();
    }

    @Test
    public void testGeneralServerSideWork() throws Exception {
        deleteBackup();

        TorrentTracker tracker = new TorrentTracker();
        tracker.start();
        Thread.sleep(100);

        Socket socket1 = new Socket(SERVER_HOST, SERVER_PORT);
        short firstPort = 1001;
        String fileName = "some_name";
        long fileSize = 100500L;
        doUpload(socket1, fileName, fileSize);

        Thread.sleep(100);
        DataInputStream in1 = new DataInputStream(socket1.getInputStream());
        assertEquals(0, in1.readInt());

        doUpdate(socket1, firstPort, Lists.newArrayList(0));
        Thread.sleep(100);
        assertEquals(true, in1.readBoolean());

        Socket socket2 = new Socket(SERVER_HOST, SERVER_PORT);
        short secondPort = 1002;
        doUpdate(socket2, secondPort, Lists.newArrayList(0));
        Thread.sleep(100);

        Socket socket3 = new Socket(SERVER_HOST, SERVER_PORT);
        DataInputStream in = new DataInputStream(socket3.getInputStream());

        doList(socket3);
        if (!waitForResponse(in)) {
            throw new TimeoutException("no response from server");
        }

        // list query check
        assertEquals(1, in.readInt());
        assertEquals(0, in.readInt());
        assertEquals(fileName, in.readUTF());
        assertEquals(fileSize, in.readLong());

        doSources(socket3, 0);

        byte ip[] = new byte[4];
        short port;

        // source query check
        assertEquals(2, in.readInt());
        for (int i = 0; i < 2; i++) {
            assertEquals(4, in.read(ip));
            assertArrayEquals(socket1.getInetAddress().getAddress(), ip);
            port = in.readShort();
            assertTrue((port == firstPort) || (port == secondPort));
        }

        tracker.stop();
        deleteBackup();
    }

    private void deleteBackup() {
        File backup = new File("server.bak");
        if (backup.exists()) {
            assertEquals(true, backup.delete());
        }
    }

    private void doList(Socket socket) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeByte(GlobalConstans.LIST_QUERY_CODE);
        out.flush();
    }

    private void doUpload(Socket socket, String name, Long size) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeByte(GlobalConstans.UPLOAD_QUERY_CODE);
        out.writeUTF(name);
        out.writeLong(size);
        out.flush();
    }

    private void doSources(Socket socket, int id) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeByte(GlobalConstans.SOURCES_QUERY_CODE);
        out.writeInt(id);
        out.flush();
    }

    private void doUpdate(Socket socket, short port, List<Integer> ids) throws IOException {
        DataOutputStream out = new DataOutputStream((socket.getOutputStream()));

        out.writeByte(GlobalConstans.UPDATE_QUERY_CODE);
        out.writeShort(port);
        out.writeInt(ids.size());
        for (int id : ids) {
            out.writeInt(id);
        }
        out.flush();
    }

    private boolean waitForResponse(DataInputStream in) throws Exception { // 1 second max
        for (int n = 0; n < 10; n++) {
            if (in.available() != 0)
                return true;
            Thread.sleep(100);
        }
        return false;
    }

}