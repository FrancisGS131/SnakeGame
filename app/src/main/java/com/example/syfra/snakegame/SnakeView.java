package com.example.syfra.snakegame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class SnakeView extends SurfaceView implements Runnable {
    //All the code will run separately to the UI
    private Thread mThread = null;
    /*
        This variable determines when the game is playing. It is declared
        volatile because it can be accessed from inside and outside the thread
    */
    private volatile boolean mPlaying;

    //This is what we draw on
    private Canvas mCanvas;
    //This is required by the Canvas class to do the drawing
    private SurfaceHolder mHolder;
    //This lets us control our colors etc
    private Paint mPaint;

    //This will be a reference to the Activity
    private Context mContext;

    //Sound
    private SoundPool mSoundPool;
    private int mGetMouseSound = -1;
    private int mDeadSound = -1;
    private int themeSong = -1;

    //For tracking movement
    public enum Direction {UP,RIGHT,DOWN,LEFT}
    //Start by heading to the right
    private Direction mDirection = Direction.RIGHT;

    //What is the screen resolution
    private int mScreenWidth;
    private int mScreenHeight;

    //Control pausing between updates
    private long mNextFrameTime;
    //Update the game 10 times per second
    private final long FPS = 10;
    //Conversion Factor -> ms to s
    private final long MILLIS_IN_A_SECOND = 1000;
    //We will draw the frame much more often

    //The current score
    private int mScore;

    //Location in the grid of all the segments
    private int[] mSnakeXs;
    private int[] mSnakeYs;

    //How long the snake is at a moment
    private int mSnakeLength;

    //Where the mouse is
    private int mMouseX;
    private int mMouseY;

    //The size in pixels of a snake segment
    private int mBlockSize;

    //The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh; //determined dynamically

    //Adding Pause
    public boolean pauseStatus = false;

    //Improving Movement
    private float initXCoordinates = -1;
    private float initYCoordinates = -1;
    private float finalXCoordinates = -1;
    private float finalYCoordinates = -1;

    //******Constructor*******
    public SnakeView(Context context, Point size){
        super(context);

        mContext = context;
        mScreenWidth = size.x;
        mScreenHeight = size.y;

        //Determine the size of each block/place on the game board
        mBlockSize = mScreenWidth/NUM_BLOCKS_WIDE;
        mNumBlocksHigh = mScreenHeight/mBlockSize;

        //Set the sound up
        loadSound();

        //Initialize the drawing objects
        mHolder = getHolder();
        mPaint = new Paint();

        //If you score over 200 you are rewarded with a crash achievement
        mSnakeXs = new int[200];
        mSnakeYs = new int[200];

        //Start the game
        startGame();
    }

    @Override
    public void run(){
        //Prevents a crash at the start. Can also extend to a pause feature
        while(mPlaying){
            //Update 10 times a second
            if(pauseStatus)
                drawGame();
            else if(checkForUpdate()){
                updateGame();
                drawGame();
            }
        }
    }

    public void pause(){
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e){
            Log.e("Error","Error in pausing game");
        }
    }

    public void resume(){
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void startGame(){
        //Start with just a head in the middle of the screen
        mSnakeLength = 1;
        mSnakeXs[0] = NUM_BLOCKS_WIDE/2;
        mSnakeYs[0] = mNumBlocksHigh/2;

        //And a mouse to eat
        spawnMouse();

        //Reset the score
        mScore = 0;

        //Setup the next time frame so an update is triggered immediately
        mNextFrameTime = System.currentTimeMillis();
        pauseStatus = true;
    }

    public void loadSound(){
        mSoundPool = new SoundPool(10,AudioManager.STREAM_MUSIC,0);
        try {
            //Create objects of the 2 required classes
            //Use mContext because this is a reference to the Activity
            AssetManager assetManager = mContext.getAssets();
            AssetFileDescriptor descriptor;

            //Prepare the two sounds in memory
            descriptor = assetManager.openFd("get_mouse_sound.ogg");
            mGetMouseSound = mSoundPool.load(descriptor,0);

            descriptor = assetManager.openFd("death_sound.ogg");
            mDeadSound = mSoundPool.load(descriptor,0);
        } catch (IOException e){
            Log.e("Error","Error in loading sounds");
        }
    }

    public void spawnMouse(){
        Random random = new Random();
        boolean change = true;
        boolean error = false;
        mMouseX = random.nextInt(NUM_BLOCKS_WIDE-1)+1;
        mMouseY = random.nextInt(mNumBlocksHigh-1)+1;

        //Food won't spawn on the snake's position
        while(change) {
            for (int i = 0; i < mSnakeXs.length; i++)
                if (mMouseX == mSnakeXs[i] && mMouseY == mSnakeYs[i])
                    error = true;
            if(error){
                mMouseX = random.nextInt(NUM_BLOCKS_WIDE-1)+1;
                mMouseY = random.nextInt(mNumBlocksHigh-1)+1;
            }
            else
                change = false;
        }
    }

    public void eatMouse(){
        //Got one, increase the size of snake
        mSnakeLength++;
        //Replace the mouse
        spawnMouse();
        //increase the score
        mScore++;
        mSoundPool.play(mGetMouseSound,1,1,0,0,1);
    }

    private void moveSnake(){
        //Move the body
        for(int i = mSnakeLength; i>0 ; i--){
            //Start at the back and move it
            mSnakeXs[i] = mSnakeXs[i-1];
            mSnakeYs[i] = mSnakeYs[i-1];

            //Exclude the head because it had nothing in front of it
        }

        //Move the head in the appropriate mDirection
        switch (mDirection){
            case UP:
                mSnakeYs[0]--;
                break;

            case DOWN:
                mSnakeYs[0]++;
                break;

            case LEFT:
                mSnakeXs[0]--;
                break;

            case RIGHT:
                mSnakeXs[0]++;
                break;
        }
    }

    private boolean detectDeath(){
        boolean dead = false;

        if(mSnakeXs[0] == -1) dead = true;
        if(mSnakeXs[0] == NUM_BLOCKS_WIDE) dead = true;
        if(mSnakeYs[0] == -1) dead=true;
        if(mSnakeYs[0] == mNumBlocksHigh) dead = true;

        for(int i=mSnakeLength-1; i>0; i--){
            int bodyX = mSnakeXs[i];
            int bodyY = mSnakeYs[i];
            if(i>4 && mSnakeXs[0] == bodyX && mSnakeYs[0] == bodyY) dead = true;
        }
        return dead;
    }

    public void updateGame(){
        //Did the head of the snake touch the mouse?
        if(mSnakeXs[0] == mMouseX && mSnakeYs[0] == mMouseY)
            eatMouse();
        moveSnake();
        if(detectDeath()){
            //Start again
            mSoundPool.play(mDeadSound,1,1,0,0,1);
            startGame();
        }
    }

    public void drawGame(){
        //Prepare to draw
        if(mHolder.getSurface().isValid()){
            mCanvas = mHolder.lockCanvas();

            //Clear the screen with a color
            mCanvas.drawColor(Color.argb(255,80,50,255));

            //Set the color of the paint to draw the snake and mouse with
            mPaint.setColor(Color.argb(255,255,255,255)); //White

            //Choose how big the score will be
            mPaint.setTextSize(60);
            mCanvas.drawText("Score: "+mScore,25,60,mPaint);

            //Draw the snake
            for(int i = 0; i<mSnakeLength; i++){
                int leftRect = mSnakeXs[i] * mBlockSize;
                int topRect = mSnakeYs[i] * mBlockSize;
                int rightRect = leftRect + mBlockSize;
                int botRect = topRect + mBlockSize;

                mCanvas.drawRect(leftRect,topRect,rightRect,botRect,mPaint);
            }

            //Draw the mouse
            int leftRect = mMouseX * mBlockSize;
            int topRect = mMouseY * mBlockSize;
            int rightRect = leftRect + mBlockSize;
            int botRect = topRect + mBlockSize;
            mCanvas.drawRect(leftRect,topRect,rightRect,botRect,mPaint);

            //Draw the whole frame
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    public boolean checkForUpdate(){
        //Are we due to update the frame?
        if(mNextFrameTime <= System.currentTimeMillis()){
            //Tenth of a second has passed

            //Setup when the next update will be triggered
            mNextFrameTime = System.currentTimeMillis() + MILLIS_IN_A_SECOND/FPS;

            //Return true so that the update and draw functions are executed
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
                if(pauseStatus)
                    pauseStatus = false;
                initXCoordinates = motionEvent.getX();
                initYCoordinates = motionEvent.getY();
            case MotionEvent.ACTION_UP:
                finalXCoordinates = motionEvent.getX();
                finalYCoordinates = motionEvent.getY();
        }
        //System.out.println("-------PASSED------");
        System.out.println("X Coords: "+initXCoordinates+" | Y Coords: "+initYCoordinates);
        System.out.println("Final X Coords: "+finalXCoordinates+" | Final Y Coords: "+finalYCoordinates);


        float xDifference = finalXCoordinates - initXCoordinates;
        float yDifference = finalYCoordinates - initYCoordinates;
        boolean xSwipe = Math.abs(xDifference) > Math.abs(yDifference);

        if(xDifference == 0 && yDifference == 0){}
        else if(xSwipe){
            if(finalXCoordinates > initXCoordinates) {
                mDirection = Direction.RIGHT;
                System.out.println("RIGHT!");
            }
            else {
                mDirection = Direction.LEFT;
                System.out.println("LEFT!");
            }
        }
        else{
            if(finalYCoordinates > initYCoordinates) {
                mDirection = Direction.DOWN;
                System.out.println("DOWN!");
            }
            else {
                mDirection = Direction.UP;
                System.out.println("UP!");
            }
        }
        return true;
    }
}
