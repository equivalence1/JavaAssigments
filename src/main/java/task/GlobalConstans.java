package task;

/**
 * Created by equi on 25.04.16.
 *
 * @author Kravchenko Dima
 */
public class GlobalConstans {
    public static final short PORT = 8081;
    public static final String SERVER_BACKUP_FILE = "server.bak";

    public static final String CLIENT_BACKUP_FILE = "client.bak";

    public static final int UPDATE_TL = 5 * 60; // 5 min

    public static final long PART_SIZE = 10 * 1024 * 1024; // 10M

    public static final byte LIST_QUERY_CODE     = 1;
    public static final byte UPLOAD_QUERY_CODE   = 2;
    public static final byte SOURCES_QUERY_CODE  = 3;
    public static final byte UPDATE_QUERY_CODE   = 4;
}
