package com.helper.robot.homebot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Created by user on 11/20/2018.
 */

public class JoyStick extends SurfaceView implements  SurfaceHolder.Callback, View.OnTouchListener{
    public JoyStick(Context context){

        super(context);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }




    public JoyStick(Context context, AttributeSet attributes, int style){

        super(context, attributes, style);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }
    private float baseX;
    private float baseY;
    private float baseR;
    private float hatR;
    private JoystickListener joystickCallback;

    private void drawJoystick(float newX, float newY){
        if(getHolder().getSurface().isValid()){
            Canvas canvas = this.getHolder().lockCanvas();
            Paint color = new Paint();
            canvas.drawARGB(255,48,48,48);
            color.setARGB(255,120,120,120);
            canvas.drawCircle(baseX, baseY, baseR, color);
            color.setARGB(255,200,200,200);
            canvas.drawCircle(newX, newY, hatR, color);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }
    public  JoyStick(Context context, AttributeSet attributes){

        super(context, attributes);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder){
        setupDimensions();
        drawJoystick(baseX, baseY);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder){

    }
    private void setupDimensions(){
        baseX = getWidth()/2;
        baseY = getHeight()/2;
        baseR = Math.min(getWidth(), getHeight())/2;
        hatR = Math.min(getWidth(), getHeight())/3;
    }
    public boolean onTouch(View v, MotionEvent e){

        if(v.equals(this)){
            if(e.getAction() == e.ACTION_DOWN || e.getAction() == e.ACTION_MOVE) {
                float displacement = (float) Math.sqrt(((Math.pow(e.getX() - baseX, 2)) + Math.pow(e.getY() - baseY, 2)));
                if (displacement < baseR) {
                    drawJoystick(e.getX(), e.getY());

                    if(e.getX()>baseR && e.getY()<baseR) //1st quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(e.getX() - baseR,2)+ Math.pow(baseR - e.getY(),2));
                        String speedAB = Float.toString(speed/baseR);
                        String speedCD = Float.toString(((baseR - e.getY())/baseR));
                        joystickCallback.onJoystickMoved( "forward", speedAB , speedCD,e.getX()/baseR/2,e.getY()/baseR/2);
                    }
                    else if(e.getX()>baseR && e.getY()>baseR) //2nd quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow( e.getX()- baseR ,2)+ Math.pow(e.getY() - baseR,2));
                        String speedAB = Float.toString(speed/baseR);
                        String speedCD = Float.toString(((e.getY() - baseR)/baseR));
                        joystickCallback.onJoystickMoved( "backward", speedAB ,speedCD,e.getX()/baseR/2,e.getY()/baseR/2);
                    }
                    else if(e.getX()<baseR && e.getY()>baseR) //3rd quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(baseR - e.getX(),2)+ Math.pow(e.getY() - baseR ,2));
                        String speedAB = Float.toString(((e.getY() - baseR)/baseR));
                        String speedCD = Float.toString(speed/baseR);
                        joystickCallback.onJoystickMoved( "backward", speedAB , speedCD,e.getX()/baseR/2,e.getY()/baseR/2);
                    }
                    else if(e.getX()<baseR && e.getY()<baseR) //4th quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(baseR - e.getX(),2)+ Math.pow(baseR - e.getY(),2));
                        String speedAB = Float.toString(((baseR - e.getY())/baseR));
                        String speedCD = Float.toString(speed/baseR);
                        joystickCallback.onJoystickMoved( "forward", speedAB , speedCD,e.getX()/baseR/2,e.getY()/baseR/2);
                    }

                } else {
                    float ratio = baseR / displacement;
                    float constrainX = baseX + (e.getX() - baseX) * ratio;
                    float constrainY = baseY + (e.getY() - baseY) * ratio;
                    drawJoystick(constrainX, constrainY);

                    if(constrainX > baseR && constrainY < baseR) //1st quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(constrainX - baseR,2)+ Math.pow(baseR - constrainY,2));
                        String speedAB = Float.toString(speed/baseR);
                        String speedCD = Float.toString(((baseR - constrainY)/baseR));
                        joystickCallback.onJoystickMoved( "forward",speedAB ,speedCD,constrainX/baseR/2,constrainY/baseR/2);
                    }
                    else if(constrainX > baseR && constrainY > baseR) //2nd quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow( constrainX - baseR ,2)+ Math.pow(constrainY - baseR,2));
                        String speedAB = Float.toString(speed/baseR);
                        String speedCD = Float.toString(((constrainY - baseR)/baseR));
                        joystickCallback.onJoystickMoved( "backward", speedAB,speedCD,constrainX/baseR/2,constrainY/baseR/2);
                    }
                    else if(constrainX < baseR && constrainY > baseR) //3rd quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(baseR - constrainX ,2)+ Math.pow(constrainY - baseR ,2));
                        String speedAB = Float.toString(((constrainY - baseR)/baseR));
                        String speedCD = Float.toString(speed/baseR);
                        joystickCallback.onJoystickMoved( "backward", speedAB ,speedCD,constrainX/baseR/2,constrainY/baseR/2);
                    }
                    else if(constrainX < baseR && constrainY < baseR) //4th quadrant
                    {
                        float speed = (float) Math.sqrt(Math.pow(baseR - constrainX ,2)+ Math.pow(baseR - constrainY ,2));
                        String speedAB = Float.toString(((baseR - constrainY)/baseR));
                        String speedCD = Float.toString(speed/baseR);
                        joystickCallback.onJoystickMoved( "forward", speedAB ,speedCD,constrainX/baseR/2,constrainY/baseR/2 );
                    }

                }
            }else if(e.getAction() == e.ACTION_UP)
            {
                drawJoystick(baseX, baseY);
                float xx = 0;
                float yy = 0;
                joystickCallback.onJoystickMoved(" ","0","0",xx ,yy);
            }
        }
        return true;
    }
    public interface JoystickListener
    {
        void onJoystickMoved( String direction, String speedAB, String speedCD, Float x,Float y);
    }


}

