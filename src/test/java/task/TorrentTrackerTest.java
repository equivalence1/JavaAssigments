package task;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTrackerTest {
    private static final String serverHost = "localhost";
    private static final short serverPort = 8081;

    @Test
    public void testStartAndStop() throws Exception {
        TorrentTracker tracker = new TorrentTracker();
        assertEquals(TorrentTracker.TrackerStatus.STOPPED, tracker.status);
        tracker.start();
        Thread.sleep(100);
        assertEquals(TorrentTracker.TrackerStatus.RUNNING, tracker.status);
        tracker.stop();
        assertEquals(TorrentTracker.TrackerStatus.STOPPED, tracker.status);
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

        byte ip[] = new byte[4];
        boolean isPeer[] = new boolean[4];

        assertEquals(2, in.readInt());
        for (int i = 0; i < 2; i++) {
            in.read(ip);
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

}