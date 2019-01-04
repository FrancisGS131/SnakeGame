package com.example.syfra.snakegame;

import android.app.Activity;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;

public class SnakeActivity extends Activity {
    SnakeView snakeView;
    MediaPlayer themeSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeSong = MediaPlayer.create(this,R.raw.snakethemesong);
        themeSong.setLooping(true);

        //Volume
        int maxVolume = 50;
        float log1=(float)(Math.log(maxVolume-30)/Math.log(maxVolume));
        themeSong.setVolume(1-log1,1-log1);

        themeSong.start();

        //find out the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();

        //Load the resolution into a Point object
        Point size = new Point();
        display.getSize(size);

        //Create a new View based on the SnakeView class
        snakeView = new SnakeView(this,size);

        //Make snakeView the default view of the Activity
        setContentView(snakeView);
    }

    //Start the thread in snakeView when this Activity is shown to the player
    @Override
    protected void onResume(){
        super.onResume();
        snakeView.resume();
        themeSong.start();
    }

    //Make sure the thread in snakeView is stopped if this Activity is about to be closed
    @Override
    protected void onPause(){
        super.onPause();

        snakeView.pause();
        snakeView.pauseStatus = true;
        themeSong.pause();
    }
}
