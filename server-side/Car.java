import java.net.*;
public class Car{
    private String username;
    private String password;
    private Server server_session;
    private Socket socket;

    public Boolean Connected = false;
    public Boolean InUse = false;

    public Car(String name, String pass, Server server, Socket sock){
        username = name;
        password = pass;
        server_session = server;
        socket = sock;
    }
    public String getName(){
        return username;
    }

}