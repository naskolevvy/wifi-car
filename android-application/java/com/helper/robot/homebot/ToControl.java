package com.helper.robot.homebot; /**
 * Created by user on 11/20/2018.
 */
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

public class ToControl extends YouTubeBaseActivity implements JoyStick.JoystickListener, YouTubePlayer.OnInitializedListener{

    private TextView details,name,identif,command,battery;
    private ImageButton swap,up,down,grab_release;
    private Switch camera,lights;
    private YouTubePlayerView youTubePlayerView;
    YouTubePlayer youTubePlayer;
    private ImageView cam;
    Login login;
    boolean isBinded = false;
    private String API_KEY = "AIzaSyDHOae9djWKou3vqziHfTIjmghM8jA7RHc";
    private String videoID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(ToControl.this, Login.class);
        bindService(intent,networkJob, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_to_control);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //set orientation to landscape



        Intent info = getIntent();
        identif = (TextView) findViewById(R.id.identification);
        command = (TextView) findViewById(R.id.command);//test
        name = (TextView)findViewById(R.id.nameOfCar);//test
        battery = (TextView) findViewById(R.id.batteryLevel);
        details = (TextView) findViewById(R.id.details);
        swap = (ImageButton) findViewById(R.id.swap);
        grab_release = (ImageButton)findViewById(R.id.grab_release);
        up = (ImageButton)findViewById(R.id.UP);
        down = (ImageButton)findViewById(R.id.DOWN);
        camera = (Switch)findViewById(R.id.camera);
        lights = (Switch)findViewById(R.id.lights);
        youTubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player);
        cam = (ImageView) findViewById(R.id.cameraimage);
        swap.setTag("1");
        grab_release.setTag("1");

        home();
        control();
        grab_release();
        up();
        down();
        camera();
        lights();

        identif.setText(info.getStringExtra("identification"));
        name.setText("username: "+info.getStringExtra("username")+" password: "+info.getStringExtra("password"));
        details.setText("Controlling the Robot!");
    }
    private BroadcastReceiver bReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            videoID = intent.getStringExtra("videoID");
           // battery.setText(videoID);
            System.out.println("eeeeEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE+ "+videoID);
        }
    };

    protected void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("video"));
    }

    protected void onPause (){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }



    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        this.youTubePlayer = youTubePlayer;
        System.out.print(videoID+"    AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        if (!b && videoID != null){
                youTubePlayer.loadVideo(videoID);
            }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(getApplicationContext(),"FUCK",Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBinded) {
            unbindService(networkJob);
            isBinded = false;
        }
    }

    private void sendMessage(String command, String part1, String part2,String part3){
        login.sendMessage(command, part1,  part2, part3);
    }

    @Override
    public void onJoystickMoved(String direction, String speedAB, String speedCD, Float xx, Float yy) {

            if(swap.getTag() == "1") {

                sendMessage("RobotMovement,",speedAB+",",speedCD+",", direction);
                command.setText("car: d: " + direction + " SAB: " + speedAB + " SCD: " + speedCD + "X: "+ xx+ "Y: "+yy);
            }else
            {
                sendMessage("ArmMovement,",xx+",",yy+",",direction);
                command.setText("arm: d: " + direction + " SAB: " + speedAB + " SCD: " + speedCD + "X: "+ xx+ "Y: "+yy);
            }
    }

    private void home(){
        ImageButton home = (ImageButton) findViewById(R.id.home);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myService = new Intent(ToControl.this, Login.class);
                login.closeConnection();
                stopService(myService);
                Intent intent = new Intent(ToControl.this, MainActivity.class);
                intent.putExtra("error","LoggedOff!");
                startActivity(intent);
            }
        });
    }

    private void camera(){
        camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    Toast.makeText(getApplicationContext(), "Camera is ON\n", Toast.LENGTH_SHORT).show();
                    sendMessage( "CAMERA,", "ON,", " ,", " ");
                    youTubePlayerView.initialize(API_KEY,ToControl.this); //start the stream
                    command.setText("Camera On");

                } else {
                    Toast.makeText(getApplicationContext(), "Camera is OFF\n", Toast.LENGTH_SHORT).show();
                    sendMessage( "CAMERA,", "OFF,", " ,", " ");
                    command.setText("Camera Off");
                    try{
                        youTubePlayer.release();
                    }catch (Exception e){
                        System.out.println(e);
                    }

                }

            }
        });
    }

    private void lights(){
        lights.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                        Toast.makeText(getApplicationContext(), "Lights are ON!\n", Toast.LENGTH_SHORT).show();
                   sendMessage( "LIGHTS,", "ON,", " ,", " ");
                    command.setText("Lights On");
                } else {

                        Toast.makeText(getApplicationContext(), "Lights are OFF!\n", Toast.LENGTH_SHORT).show();
                   sendMessage( "LIGHTS,", "OFF,", " ,", " ");
                    command.setText("Lights Off");

                }
            }
        });
    }

    private void control(){
        swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (swap.getTag() == "1") {
                    details.setText("Controlling the Arm!");
                    swap.setImageResource(R.drawable.car1);
                    Toast.makeText(getApplicationContext(), "Controlling the Arm!\n", Toast.LENGTH_SHORT).show();
                    swap.setTag("2");
                }else
                {
                    swap.setImageResource(R.drawable.arm3);
                    details.setText("Controlling the Robot!");
                    Toast.makeText(getApplicationContext(), "Controlling the Car!\n", Toast.LENGTH_SHORT).show();
                    swap.setTag("1");
                }
            }
        });
    }

    private void grab_release() {
        grab_release.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    if (grab_release.getTag() == "1") {
                        grab_release.setImageResource(R.drawable.release);
                        Toast.makeText(getApplicationContext(), "You just grabbed!\n", Toast.LENGTH_SHORT).show();
                        sendMessage("GRAB,","0,"," ,"," ");
                        command.setText("Grab");
                        grab_release.setTag("2");
                    } else {
                        grab_release.setImageResource(R.drawable.grab);
                        Toast.makeText(getApplicationContext(), "You just released!\n", Toast.LENGTH_SHORT).show();
                        sendMessage("RELEASE,","180,"," ,"," ");
                        command.setText("Release");
                        grab_release.setTag("1");
                    }

            }
        });
    }

    private void up(){

        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                sendMessage( "UP,", "1,", " ,", " ");
                command.setText("Up + 1");

            }
        });

        up.setOnLongClickListener(
                new View.OnLongClickListener(){
                    public boolean onLongClick(View arg0) {
                        goUp = true;
                        repeatUpdateHandler.post( new RptUpdater() );
                        return false;
                    }
                }
        );

        up.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && goUp ){
                    goUp = false;
                }
                return false;
            }
        });

    }

    private void down(){

        down.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {


                sendMessage( "DOWN,", "1,", " ,"," ");
                command.setText("Down -1");
            }


        });
        down.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                goDown = true;
                repeatUpdateHandler.post( new RptUpdater() );
                return false;
            }
        });
        down.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && goDown ){
                    goDown = false;
                }
                return false;
            }
        });
    }
//all this above is needed to do something while the button is pressed

    private Handler repeatUpdateHandler = new Handler();
    private boolean goDown = false;
    private boolean goUp = false;


    class RptUpdater implements Runnable {
        public void run() {
            if( goDown ){
                sendDOWN();
                repeatUpdateHandler.postDelayed( new RptUpdater(), 50 );
            } else if( goUp ){
                sendUP();
                repeatUpdateHandler.postDelayed( new RptUpdater(), 50 );
            }
        }
    }
    public void sendUP(){

            sendMessage( "UP,", "1,", " ,", " ");
            command.setText("Up + 1");
    }
    public void sendDOWN(){

            sendMessage( "DOWN,", "1,", " ,"," ");
            command.setText("Down -1");

    }

}