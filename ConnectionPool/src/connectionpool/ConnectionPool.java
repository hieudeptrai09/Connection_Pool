package connectionpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionPool implements Pool {

    private final ServerSocket server = new ServerSocket();
    private LinkedList<Connection> pool;
    private int capacity;
    private final int DEFAULT_CAPACITY = 10;
    private final int MAX_CAPACITY = 30;
    private final int DEADLINE = 5;
    private final int CONFIRMING_CODE = 29;
    private final int REPCODE = 94;
    private LinkedList<Integer> notRep;

    public ConnectionPool() throws IOException {
        this.capacity = DEFAULT_CAPACITY;
        pool = new LinkedList<>();
        notRep = new LinkedList<>();
    }

    public ConnectionPool(int capacity) throws OutOfCapacityException, IOException {
        if (capacity <= MAX_CAPACITY) {
            this.capacity = capacity;
            pool = new LinkedList<>();
            notRep = new LinkedList<>();
        } else {
            throw new OutOfCapacityException();
        }
    }

    @Override
    public void add() throws OutOfCapacityException, IOException {
        if (pool.size() < capacity) {
            server.bind(new InetSocketAddress(Math.abs(new Random().nextInt() % 65536)));
            Connection conn = new Connection(server.accept());
            conn.run();
            pool.add(new Connection(server.accept()));
        } else {
            throw new OutOfCapacityException();
        }
    }

    private void remove(int index) throws IOException {
        Connection conn = pool.get(index);
        if (conn != null) {
            conn.getSocket().close();
            pool.remove(index);
            notRep.remove(index);
        }
    }

    @Override
    public void terminate() throws IOException {
    }
    
    public void maintain() throws IOException {
        new Sweep().run();
        for(int i = 0; i < pool.size(); i++) {
            if (notRep.get(i) == DEADLINE) {
                notRep.remove(i);
                remove(i);
            }
        }
    }

    class Sweep implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < pool.size(); i++) {
                try {
                    DataOutputStream dos = new DataOutputStream(pool.get(i).getSocket().getOutputStream());
                    dos.writeByte(CONFIRMING_CODE);
                    DataInputStream dis = new DataInputStream(pool.get(i).getSocket().getInputStream());
                    int rep = dis.readByte();
                    if (rep == REPCODE) {
                        notRep.set(i, 0);
                    }
                    else {
                        notRep.set(i, notRep.get(i)+1);
                    }
                    sleep(10000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    
    class Connection implements Runnable {
        
        private Socket socket;

        public Connection(Socket socket) {
            this.socket = socket;
        }

        public Socket getSocket() {
            return socket;
        }
        
        @Override
        public void run() {
            
        }
    
    }
}


