import javax.xml.crypto.dom.DOMCryptoContext;
import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.Scanner;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.SoftPwm;

public class CarClient {
    private static int MOTOR_1_PIN_A = 4;
    private static int MOTOR_1_PIN_B = 5;
    private static int MOTOR_2_PIN_A = 0;
    private static int MOTOR_2_PIN_B = 2;
    private Process p;
    private final int PORT = 62211;
    private final String serverIP = "51.68.47.57";
    private boolean isServerConnected;
    private String username = "nasko"; //get them from a file
    private String password = "admin"; //same
    private Socket socket = null;
    private BufferedReader in;
    private PrintWriter out;
    private static double value = 0.06;
    private static double testValue = 0;
    private String videoID;  //"fDvT3dEQgmc";// "UVxU2HzPGug";
    // private Scanner input = new Scanner(System.in);

        final GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalOutput motor1pinE = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "m1E");
        final GpioPinDigitalOutput motor2pinE = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "m2E");
        final GpioPinDigitalOutput led_light = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_23,"led");
        SoftPwm mot_1a,mot_1b,mot_2a,mot_2b;


    long pid = 0;
    /**
     * Constructor
     */
    public CarClient() {}

    /**
     * This is the method from which everything is ran.
     */
    public void run() {

        mot_1a.softPwmCreate(MOTOR_1_PIN_A, 0, 100);
        mot_1b.softPwmCreate(MOTOR_1_PIN_B, 0, 100);
        mot_2a.softPwmCreate(MOTOR_2_PIN_A, 0, 100);
        mot_2b.softPwmCreate(MOTOR_2_PIN_B, 0, 100);
        motor2pinE.high();
        motor1pinE.high();

        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCjwiH-TrEG0qHEltva7oc4g&Type=live&type=video&key=AIzaSyDHOae9djWKou3vqziHfTIjmghM8jA7RHc\n");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            String res = result.toString();
            String test[] = res.split(",");
            for(int i = 0; i < test.length; i ++ ){
                if(test[i].contains("videoId")){
                    String link[] = test[i].split(":");
                    String videId = link[1].substring(0,link[1].length()-1).trim();
                    videId = videId.substring(1,videId.length()-1);
                    System.out.println(videId);
                    videoID = videId;
                }
                if(test[i].contains("liveBroadcastContent")){
                    String part[] = test[i].split(":");
                    if(part[1].contains("live")){
                        break;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultPosition();
        establishConnection();
        handleOutgoingMessages();
        handleIncomingMessages();
    }

    /**
     * This method is used to create the connection with a server
     */
    private void establishConnection() {

    do{
        try {
            socket = new Socket( serverIP, PORT ); //replace with serverIP
            in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String send = "Car_Connecting,"+username+","+password;
            out.println(send);
            isServerConnected = true;

        }
        catch (IOException e) {
            System.err.println("Exception in handleConnection(): " + e );
        }
    }while(!isServerConnected);

    }

    /**
     * This method creates a thread to wait for user input and send it to the server
     */
    private void handleOutgoingMessages() { //Sender thread
        /*
        Thread senderThread = new Thread( new Runnable() {
            public void run() {
                String send = "Car_Connecting,"+username+","+password;
                out.println(send);
            }
        });
        senderThread.start();
*/
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

    private void sendImage(){
        Thread senderThread = new Thread( new Runnable() {
            public void run() {

                try {
                    InputStream in = new FileInputStream("/home/pi/image.jpg");
                    OutputStream out = socket.getOutputStream();
                    copy(in, out);
                    out.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        senderThread.start();
    }
    /**
     * This method creates a thread to wait for response from the server
     */
    private void handleIncomingMessages() { // Listener thread
        Thread listenerThread = new Thread( new Runnable() {
            public void run() {

                String line;
                while(isServerConnected) {
                    line = null;
                    try {
                        line = in.readLine();
                        if ( line == null ) {
                            System.out.println("Disconnected from the server!");
                            isServerConnected = false;
                            closeConnection();
                            motor1pinE.low();
                            motor2pinE.low();
                            gpio.shutdown();
                            switch_off();
                            break;
                        }
                        commandReceived(line); //send the line to the function that handles the commands
                        System.out.println("\ndoide "+line);
                    }
                    catch (IOException e) {
                        isServerConnected = false;
                        System.out.println("[Error]Your connection was forcibly closed.");
                        closeConnection();
                        motor1pinE.low();
                        motor2pinE.low();
                        gpio.shutdown();
                        try {
                            switch_off();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
        listenerThread.start();
    }

    /**
     * This method closes the connection and the program.
     */
    private void closeConnection() { //method that closes the connection with the server
        try {
            gpio.shutdown();
            socket.close();
            switch_off();
            System.exit(0);
        }
        catch (IOException e) {
            System.err.println( "Exception when closing the socket!" );
            System.err.println( e.getMessage() );
        }
    }

    private void echo(String str) throws FileNotFoundException { //controls the PWM signals
        final String file = "/dev/pi-blaster";
        try (PrintWriter out = new PrintWriter(new FileOutputStream(file), true)) {
            out.println(str);

        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }
    private void commandReceived(String command) {

        String separateCommands[] = command.split(",");
        Double part1d,part2d;

        switch (separateCommands[4]){
            case "RobotMovement":
                part1d = Double.parseDouble(separateCommands[5]);
                part2d = Double.parseDouble(separateCommands[6]);
                controlMotor(part1d, part2d,separateCommands[7]);
                break;
            case "ArmMovement":
                if(separateCommands[7].equals("forward")){
                    part1d = Double.parseDouble(separateCommands[5]);
                    part2d = Double.parseDouble(separateCommands[6]);
                    horizontalArmMovement(part1d, part2d);
                    System.out.println("poluchigove");
                }

                break;
            case "CAMERA":
                try {
                    camera(separateCommands[5]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "LIGHTS":
                lights(separateCommands[5]);
                break;
            case "GRAB":
                grab();
                break;
            case "RELEASE":
                release();
                break;
            case "UP":
                verticalArmMovementUP();
                break;
            case "DOWN":
                verticalArmMovementDOWN();
                break;
        }
    }
//pin 17 for horizontal servo from 0.02 - 0.23
//pin 18 for up / down arm movement from 0.06 - 0.185
//pin 4 - forward/backward arm - 0.075 - 0.130
//pin whatever 21 - grab/release - 0.120 - 0.22

    private void defaultPosition(){
        try{
            echo("22=0.125");
            echo("18=0.06");
            echo("4=0.075");
            echo("21=0.12");

        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }

    }

    private void testu(){
        testValue = testValue + 0.005;
        System.out.println(testValue);
        String send = "4="+testValue;
        try{
            echo(send);

        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }
    private void testd(){
        testValue = testValue - 0.005;
        System.out.println(testValue);
        String send = "4="+testValue;
        try{
            echo(send);

        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }

    private void horizontalArmMovement(Double servo_ab, Double servo_cd){ //when looking if part3 is equal to something use equals()
        String send1 = "22="+(0.02+(1-servo_ab)*0.21);
        String send2 = "4="+(0.075+(1-servo_cd)*0.255);
        System.out.println(send1);
        System.out.println(send2);
        try{
            echo(send1);
            echo(send2);
        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }

    private void verticalArmMovementUP(){
        value = value + 0.005;
        String send = "18="+value;
        System.out.println(send);
        if(value >= 0.8){
            value = 0.8;
            send = "18=0.8";
        }
        try{
            echo(send);
        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }

    private void verticalArmMovementDOWN(){
        value = value - 0.0025;
        String send = "18="+value;
        System.out.println(send);
        if(value <= 0.06){
            value = 0.06;
            send = "18=0.06";
        }
        try{
            echo(send);
        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }
    }

    private void grab(){

        String send = "21=0.22";
        try{
            echo(send);
        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }


    }
    private void release(){

        String send = "21=0.05";
        try{
            echo(send);
        }catch (FileNotFoundException ex)
        {
            System.out.println(ex);
        }



    }
    private void controlMotor(Double ab, Double cd, String direc){ //function to control the speed and direction of motors
        int ab_speed = (int) Math.round(ab*100);
        int cd_speed = (int) Math.round(cd*100);
        if(direc.equals("backward")){

            mot_1a.softPwmWrite(MOTOR_1_PIN_A, ab_speed);
            mot_2a.softPwmWrite(MOTOR_2_PIN_A,cd_speed);
            mot_1b.softPwmWrite(MOTOR_1_PIN_B,0);
            mot_2b.softPwmWrite(MOTOR_2_PIN_B,0);
        }else if(direc.equals("forward")){

            mot_1a.softPwmWrite(MOTOR_1_PIN_A, 0);
            mot_2a.softPwmWrite(MOTOR_2_PIN_A,0);
            mot_1b.softPwmWrite(MOTOR_1_PIN_B,ab_speed);
            mot_2b.softPwmWrite(MOTOR_2_PIN_B,cd_speed);
            System.out.println("backward e");
        }else{
            mot_1a.softPwmWrite(MOTOR_1_PIN_A, 0);
            mot_2a.softPwmWrite(MOTOR_2_PIN_A,0);
            mot_1b.softPwmWrite(MOTOR_1_PIN_B,0);
            mot_2b.softPwmWrite(MOTOR_2_PIN_B,0);
        }
    }
    private void camera(String command) throws IOException{
        //Runtime rt = Runtime.getRuntime();
        if(command.equals("ON")){
            out.println("Car_Responding,"+username+","+password+","+videoID);
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    "raspivid -o - -t 0 -vf -hf -fps 10 -b 500000 | ffmpeg -re -ar 44100 -ac 2 -acodec pcm_s16le -f s16le -ac 2 -i /dev/zero -f h264 -i - -vcodec copy -acodec aac -ab 128k -g 50 -strict experimental -f flv rtmp://a.rtmp.youtube.com/live2/eq8c-8t9x-5rm6-av5b"
            };
            p = Runtime.getRuntime().exec(cmd);

            pid = getPidOfProcess(p) + 1;
            System.out.println("pidto na procesa: " + pid);
        }else{
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    "sudo kill "+pid
            };
            p = Runtime.getRuntime().exec(cmd);
            pid = pid + 1;
            String[] cmd2 = {
                    "/bin/sh",
                    "-c",
                    "sudo kill "+pid
            };
            p = Runtime.getRuntime().exec(cmd2);
        }
    }
    public long getPidOfProcess(Process p) {
        long pid = -1;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }
    private void lights(String command){ //function to switch the ligths on/off
        if(command.equals("ON")){
            led_light.high();
        }else{
            led_light.low();
        }
    }
    private  void switch_off() throws IOException{ //function to switch off the car
        Runtime rn = Runtime.getRuntime();
        rn.exec("sudo shutdown -h now");
    }
}