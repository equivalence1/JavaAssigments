package task.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by equi on 17.04.16.
 */
public class FileInfo {
    public int id;
    public String name;
    public long size;
    public Set<Integer> parts;

    FileInfo(int id, String name, long size) {
        this.id   = id;
        this.name = name;
        this.size = size;
        parts = new HashSet<>();
    }
}
