package com.helper.robot.homebot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    EditText usernameField;
    EditText passwordField;
    String username = "";
    String password = "";
    Login login;
    boolean isBinded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //only portrait mode
        setContentView(R.layout.activity_main);
        usernameField = (EditText)findViewById(R.id.t_username);
        passwordField = (EditText)findViewById(R.id.t_password);
        if (android.os.Build.VERSION.SDK_INT > 9) //no idea what this does but it stops an error when trying to perform network op in main thread
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Start();
        Intent intent = new Intent(MainActivity.this, Login.class);
        bindService(intent,networkJob,Context.BIND_AUTO_CREATE);
        ///startService(intent);

        Intent myIntent = getIntent(); //checks if the connection to the car have been lost
        if(myIntent.getExtras()!=null) { //prevents error
            String connectionLost = myIntent.getStringExtra("error");
            if(connectionLost!=null) {
                Intent myInt = new Intent(this,Login.class); //previous intent is from ToControl, so we need from Main

                if (connectionLost.equals("ConnectionLost!")) { //connection to car is lost
                    startService(intent);
                    intent.removeExtra("error");
                    Toast.makeText(getApplicationContext(), "Connection to car lost!", Toast.LENGTH_SHORT).show();
                } else if (connectionLost.equals("Busy!")) {
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "This Car is already taken!\n Please try again later!", Toast.LENGTH_SHORT).show();
                } else if (connectionLost.equals("WrongCredentials!")) {
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "Wrong Username or Password!", Toast.LENGTH_SHORT).show();
                }else if (connectionLost.equals("NoCarConnected!")) {
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "No Car with such credentials is available at the moment!", Toast.LENGTH_SHORT).show();
                }else if (connectionLost.equals("LoggedOff!")) {
                    startService(myInt);
                    Toast.makeText(getApplicationContext(), "You logged off successfully!", Toast.LENGTH_SHORT).show();
                }else if (connectionLost.equals("ServerOff!")) {
                    //startService(myInt);
                    Toast.makeText(getApplicationContext(), "The Server is currently Offline!", Toast.LENGTH_SHORT).show();
                }else if (connectionLost.equals("CarDisconnected!")) {
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "The Car has disconnected!", Toast.LENGTH_SHORT).show();
                }


            }
        }
    }


    private void Start(){
        Button login = (Button)findViewById(R.id.b_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                username = usernameField.getText().toString().trim(); //get the username and password from the text field
                password = passwordField.getText().toString().trim();
                if(!username.equals("") && !password.equals("")) {

                    usernameField.setText("");
                    passwordField.setText("");

                    if (connectionCheck()) {
                        Intent intent = getIntent();
                        if(intent.getStringExtra("error") != null){
                            String connectionLost = intent.getStringExtra("error");
                            if(connectionLost.equals("ServerOff!")){
                                Intent serverIntent = new Intent(MainActivity.this, Login.class);
                                bindService(serverIntent,networkJob,Context.BIND_AUTO_CREATE);
                                startService(serverIntent);
                            }
                        }
                        setLogin();
                    } else {
                        Toast.makeText(getApplicationContext(), "No Internet Connection!\n", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Enter Username or Password!\n", Toast.LENGTH_SHORT).show();
                    Start();
                    usernameField.setText("");
                    passwordField.setText("");
                }
            }
        });
    }

    private ServiceConnection networkJob = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Login.LocalService localService = (Login.LocalService)service;
            login = localService.getServices();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindService(networkJob);
            isBinded = false;
        }
    };
    private void setLogin(){
        login.appLogin(username,password);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBinded) {
            unbindService(networkJob);
            isBinded = false;
        }
    }

    private boolean connectionCheck() { //check if there is internet connection
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return true;
            }
        } else {
            return false;
        }
        return false;
    }

}
