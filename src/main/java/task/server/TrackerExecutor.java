package task.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public interface TrackerExecutor {

    void executeList(DataOutputStream out) throws IOException;

    void executeUpload(ClientInfo peer, String name, long size, DataOutputStream out) throws IOException;

    void executeSources(int id, DataOutputStream out) throws IOException;

    void executeUpdate(ClientInfo clientInfo, List<Integer> ids, DataOutputStream out) throws IOException;

}
