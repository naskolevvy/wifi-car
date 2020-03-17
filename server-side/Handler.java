import java.io.*;
import java.net.*;
import java.util.*;


public class Handler extends Thread {

    private Server server;
    private Socket client;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private String NameOfConnection =""; //used to identify the type of connected device - app, car + login details
    private String login = "";
    private String pass = "";
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    Data x = new Data(); //create an object containing the two hashmaps for the apps and the cars

    public Handler(Server server, Socket client) { //constructor
        this.server = server;
        this.client = client;
    }

    private String randomString() { //create a random string used as identifier of connection
        StringBuilder builder = new StringBuilder();
        int length = 10;
        while (length-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }
    @Override
    public void run() {
        HashMap<String , Car> CarsConnected = x.getCars(); //hashmap containing the cars
        HashMap<String , App> AppsConnected = x.getApps(); //global hashmap containing the apps
        try {
            in = new BufferedReader( new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String line;
        while(true) {
            try {
                line = in.readLine(); //input line from the app or the car
                String[] commands = line.split(","); //split it into parts
                if(commands.length>2){
                    login = commands[1]; //username of car and app, used for deleting from maps on error
                    if(commands[0].equals("Car_Connecting")) { //initial connection of the car

                        this.NameOfConnection = "Car_"+commands[1]; //set the name of the handler
                        Car car = new Car(commands[1],commands[2],server,client); //create a car object
                        CarsConnected.put(commands[1],car); //put this in the map
                        CarsConnected.get(commands[1]).Connected = true; //change the status of the car as online
                        System.out.println("Car Connected with name: "+ CarsConnected.get(commands[1]).getName());

                    }else if(commands[0].equals("Car_Responding")){
                        /*
                        System.out.println("Vleze");
                        String name = line;
                        new Thread(){
                            public void run() {
                                System.out.println("Vleze1");
                                InputStream in = null;
                                try {
                                    in = client.getInputStream();
                                    OutputStream out = new FileOutputStream("image.jpg");
                                    copy(in, out);
                                    out.close();
                                    in.close();
                                    String[] test = name.split("_");
                                    System.out.println("Vleze2");
                                    for(Handler h: server.getClients()){
                                        if(h.getType().contains("App_"+test[1])){
                                            h.send("Image");
                                            InputStream inn = new FileInputStream("image.jpg");
                                            OutputStream outt = client.getOutputStream();
                                            copy(inn,outt);
                                            inn.close();
                                            outt.close();
                                            System.out.println("Vleze3");
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();

                        */

                        for(Handler h: server.getClients()){
                            if(h.getType().contains("App_"+commands[1])){
                                h.send("Video,"+commands[3]); //send the videoID to the App
                            }
                        }

                    }else if(commands[0].equals("App_Login")) { //section where the login is handled

                        String identification = randomString(); //create the identification string
                        this.NameOfConnection = "App_"+commands[1]+"_"+identification; //set the name of the session

                        if(AppsConnected.containsKey(commands[1])){ //if there is an App with the same key already
                            if(CarsConnected.containsKey(commands[1])){
                                if(CarsConnected.get(commands[1]).getPass().equals(commands[2])){
                                    this.send("Busy!");
                                    System.out.println("App disconnected because the car is already in use!");
                                }else{
                                    this.send("WrongCredentials!");
                                    System.out.println("App disconnected because the car is in use plus wrong password!");
                                }
                            }else{
                                this.send("NoCarConnected!");
                                System.out.println("App disconnected because no car with such name is connected!");
                            }
                            server.getClients().remove(this); //remove this session from the server list
                            Thread.currentThread().interrupt();
                            break;
                        }else{ //if there is no such app connected
                            if(CarsConnected.size()>0){
                                App app = new App(commands[1],commands[2],server,client,identification); //create app object
                                AppsConnected.put(commands[1],app); //add it to the list of apps
                                if(CarsConnected.containsKey(commands[1])){ //if such car exists
                                    if(CarsConnected.get(commands[1]).getPass().equals(commands[2])){ //if the login details match
                                        CarsConnected.get(commands[1]).InUse = true; //make the car in use
                                        AppsConnected.get(commands[1]).controlling = true; //the app is currently controlling
                                        this.send("Identification,"+identification); //send the identification key
                                        System.out.println("Identification key send to: "+this.getType()+" App + key: "+ identification);
                                    }else{ //if the password is wrong
                                        this.send("WrongCredentials!");
                                        server.getClients().remove(this);
                                        AppsConnected.remove(commands[1]);
                                        System.out.println("App disconnected because wrong password!");
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }else{
                                    this.send("NoCarConnected!");
                                    server.getClients().remove(this);
                                    System.out.println("App disconnected because no such car is connected!");
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }else{ //there are no cars connected currently
                                this.send("NoCarConnected!");
                                server.getClients().remove(this);
                                System.out.println("App disconnected because no cars are connected!");
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                    }else if(commands[0].equals("App_Command")){ //section where the commands are handled

                        String name = "Car_"+commands[1]; //the name of the car the command needs to be send to
                        if(CarsConnected.containsKey(commands[1])){ //if such car exists
                            if(AppsConnected.get(commands[1]).isConnected() && AppsConnected.get(commands[1]).getIdentification().equals(commands[3])){ //if the app that sends the command is connected or not
                                for(Handler h: server.getClients()){
                                    if(h.getType().equals(name)){ //add password check, plus maybe a feature in the app hashmap to know who sends it
                                        if(CarsConnected.get(commands[1]).getPass().equals(commands[2])){ //pass check
                                            System.out.println("Command send: " + line);
                                            h.send(line);
                                        }else{ //if the pass when sending the command

                                            AppsConnected.remove(commands[1]);
                                            System.out.println("App Disconnected " + commands[1]);
                                            server.getClients().remove(this);
                                            Thread.currentThread().interrupt();
                                            break;
                                        }
                                    }
                                }
                            }else{ //app not logged into car
                                AppsConnected.remove(commands[1]);
                               server.getClients().remove(this);
                               Thread.currentThread().interrupt();
                               break;
                            }
                        }else{
                            AppsConnected.remove(commands[1]);
                            server.getClients().remove(this);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

            }catch (Exception e) { //if the connection is suddenly closed

                if(this.NameOfConnection.contains("App")){ //remove che app session
                    for(Handler q : server.getClients()) {
                        if(this.NameOfConnection.equals(q.getType())){ //check if the name of the connection matches any from the server list
                            String[] test = q.getType().split("_");
                            if (AppsConnected.containsKey(test[1])){ //if the list contains an app with that key
                                if(AppsConnected.get(test[1]).getIdentification().equals(test[2])){ //find the right and only session
                                    AppsConnected.remove(test[1]); //remove it
                                    CarsConnected.get(test[1]).InUse = false; //free the car
                                    System.out.println("App disconected: " + test[1]);
                                }
                            }
                        }
                    }

                }else if(this.NameOfConnection.contains("Car")){ //remove the correct car session

                    for(Handler q : server.getClients()) {
                        String[] test = q.getType().split("_");
                        if(this.NameOfConnection.equals(q.getType())) { //remove the car and the app from the lists
                            CarsConnected.remove(test[1]);
                            System.out.println("Car disconected: " + test[1]);
                            for (Handler h: server.getClients()) { //to notify the app that is controlling the car
                                if(h.getType().contains("App_"+test[1])){
                                    h.send("CarDisconnected!");
                                    if(AppsConnected.containsKey(test[1])) AppsConnected.remove(test[1]);
                                    server.getClients().remove(q.getType().contains("App_" + test[1]));
                                }
                            }
                        }
                    }
                }
                System.out.println("Client Disconnected " + this.NameOfConnection);
                server.getClients().remove(this); //remove the session
                Thread.currentThread().interrupt();
                break; //break the while loop
            }
        }
    }
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
    public void send(String msg) {
        out.println(msg);
        out.flush();
    }

    public String getType() {
        return this.NameOfConnection;
    }
}

