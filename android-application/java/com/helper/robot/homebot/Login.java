
package com.helper.robot.homebot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by user on 3/6/2019.
 */

public class Login extends Service {
    private final IBinder binder = new LocalService();
    private static String username = "";
    private static String password = "";
    public static String identification = "";
    private String ipServer = "51.68.47.57"; //for testing
    private  BufferedReader input;
    private  OutputStream outputStream;
    private Socket socket;
    private static boolean onConnected = false;


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new NetworkThread()).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new NetworkThread()).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnection();
        stopSelf();
    }

    public class NetworkThread implements Runnable{
        @Override
        public void run() {
            try {
                onConnected = true;
                socket = new Socket(ipServer,62211);
                outputStream = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;

                while(true){ //this is the input stream, where the server sends info to the app
                    line = input.readLine();
                    if(line == null || line.contains("!")){ //different error messages

                        Intent myIntent = new Intent(Login.this, MainActivity.class); //create intent and pass error param to main

                        if(line == null)
                        {
                            socket.close();
                            onConnected = false;
                            myIntent.putExtra("error","ConnectionLost!");
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myIntent); //start back the main activity
                            stopSelf();
                            break;
                        }
                        if(line.equals("Busy!"))
                        {
                            socket.close();
                            onConnected = false;
                            myIntent.putExtra("error","Busy!");
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myIntent); //start back the main activity
                            stopSelf();
                            break;
                        }
                        if(line.equals("WrongCredentials!"))
                        {

                            socket.close();
                            onConnected = false;
                            myIntent.putExtra("error","WrongCredentials!");
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myIntent); //start back the main activity
                            stopSelf();
                            break;
                        }
                        if(line.equals("NoCarConnected!"))
                        {
                            socket.close();
                            onConnected = false;
                            myIntent.putExtra("error","NoCarConnected!");
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myIntent); //start back the main activity
                            stopSelf();
                            break;
                        }
                        if(line.equals("CarDisconnected!"))
                        {
                            socket.close();
                            onConnected = false;
                            myIntent.putExtra("error","CarDisconnected!");
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myIntent); //start back the main activity
                            stopSelf();
                            break;
                        }
                    }
                    if(line.contains("File")){ //test statement for sending files

                    }
                    if(line.contains("Video,")){
                        String[] videoLink = line.split(",");
                        sendBroadcast(videoLink[1]);
                    }

                    if(line.contains("Identification,")){
                        String[] ident = line.split(",");
                        identification = ident[1];
                        Intent intent = new Intent(Login.this, ToControl.class); //pass them to network class
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                        intent.putExtra("identification", identification);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }

            } catch (IOException e){ //if no connection to the socket go to Main

                Intent myIntent = new Intent(Login.this, MainActivity.class); //create intent and pass error param to main
                myIntent.putExtra("error","ServerOff!");
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(myIntent);
                onConnected = false;
                stopSelf();
                e.printStackTrace();
            }
        }
    }

    public class LocalService extends Binder{
        Login getServices(){
            return Login.this;
        }
    }

    public void appLogin(String user, String pass){ //try to login
        username = user;
        password = pass;
        String msg = "App_Login,"+username+","+password+ "\n";
        try {
            if(onConnected){
                System.out.println(msg);
                outputStream.write(msg.getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            closeConnection();
            stopSelf();
            e.printStackTrace();
        }
    }
    public void sendMessage(String command, String part1, String part2,String part3){
        String msg = "App_Command,"+username+","+password+","+identification+","+ command+part1+part2+part3+"\n";
        System.out.println(msg);
        try {
            outputStream.write(msg.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            closeConnection();
            stopSelf();
            e.printStackTrace();
        }
    }
    public void closeConnection(){
        try {
            socket.close();
            onConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
    private void sendBroadcast (String videoID){
        Intent intent = new Intent ("video"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("videoID", videoID);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
