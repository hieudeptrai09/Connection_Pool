package connectionpool;

import java.io.IOException;
import java.net.Socket;

interface Pool {
    public void add () throws OutOfCapacityException, IOException;
    public void terminate () throws IOException;
}
