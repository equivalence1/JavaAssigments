package task.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by equi on 17.04.16.
 */
public class FileInfo {
    public int id;
    public String name;
    public String path;
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
