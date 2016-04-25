package task.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */
public class FileInfo {
    public int id;
    public String name;
    public String path; // path can be != name
    public long size;
    public Set<Integer> parts;

    FileInfo(int id, String name, String path, long size) {
        this.id   = id;
        this.name = name;
        this.path = path;
        this.size = size;
        parts = new HashSet<>();
    }
}
