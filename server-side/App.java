import java.net.*;

public class App{
    private String username;
    private String password;
    private Server server_session;
    private Socket socket;
    public String identification;
    public Boolean controlling = false;

    public App(String name, String pass, Server server, Socket sock, String identification){
        username = name;
        password = pass;
        server_session = server;
        socket = sock;
        this.identification = identification;
    }
    public String getName(){
        return username;
    }
    public String getPass(){
        return password;
    }
    public Server getServer(){
        return server_session;
    }
    public Socket getSocket(){
        return socket;
    }
    public Boolean isConnected(){
        return controlling;
    }
    public String getIdentification(){
        return identification;
    }
}