import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private ServerSocket ss;
    private final int  PORT = 62211;
    private Set<Handler> clients = new HashSet<Handler>();
    public Server() {}

    public void start() {
        try {
            ss = new ServerSocket(PORT);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Server waiting for connections . . .");

        Socket socket;
        Handler client;
        try{
            while(true) {
                socket = ss.accept(); 	// accepting and adding the sessions
                client = new Handler(this, socket);
                clients.add(client);
                client.start();
                System.out.println("Client connected!");
            }
        }
        catch(Exception e) {
            System.out.println( e.getMessage() );
        }
    }

    public Set<Handler> getClients() {
        return this.clients;
    }

}

