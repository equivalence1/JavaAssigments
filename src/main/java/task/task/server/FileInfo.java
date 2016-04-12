package task.task.server;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by equi on 08.04.16.
 *
 * @author Kravchenko Dima
 */
public class FileInfo {
    private int id;
    private List<ClientInfo> activeClientInfos;

    public FileInfo() {
        activeClientInfos = new LinkedList<>();
    }

    public int getId() {
        return id;
    }

    public List<ClientInfo> getActiveClientInfos() {
        return activeClientInfos;
    }
}
