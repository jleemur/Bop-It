package mobiledev.unb.ca.bopit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.List;
import java.util.Random;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tbouron.shakedetector.library.ShakeDetector;

public class Game extends Activity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{

    private static final String DEBUG_TAG = "DEBUG";
    private DBHelper mDBHelper;
    private GestureDetectorCompat mDetector;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static int correctAction = 0;
    private ImageView img_view;
    private int score = 0;
    private int volume = 0;
    private TextView scoreText;
    private CountDownTimer timer;
    private CountDownTimer scoretimer;
    private CountDownTimer failTimer;
    private MediaPlayer mp;
    private MediaPlayer mp1;
    private ImageView star;
    private ImageView bVolume;
    private int upper_timer = 3000;
    private int lower_timer = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        mDetector = new GestureDetectorCompat(this,this);
        mDBHelper = new DBHelper(this);

        // Set the gesture detector as the double tap listener.
        mDetector.setOnDoubleTapListener(this);
        img_view = (ImageView) findViewById(R.id.game_imgview);
        scoreText = (TextView) findViewById(R.id.score);
        mp = MediaPlayer.create(Game.this, R.raw.ding);
        mp1 = MediaPlayer.create(Game.this, R.raw.wrong);
        star = (ImageView) findViewById(R.id.starpts);
        star.setVisibility(View.INVISIBLE);

        bVolume = (ImageView)findViewById(R.id.img_volume);
        bVolume.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(volume == 0){
                    mp.setVolume(0,0);
                    volume++;
                    bVolume.setImageResource(R.drawable.novolume);
                }else{
                    mp.setVolume(1,1);
                    volume--;
                    bVolume.setImageResource(R.drawable.volume);
                }

            }
        });


        ShakeDetector.create(this, new ShakeDetector.OnShakeListener() {
            @Override
            public void OnShake() {
                resolve(6);
            }
        });

        start();
    }

    public void start(){
        Random r = new Random();
        correctAction = r.nextInt(6) + 1; //1-5

        // 3 seconds - why is this timer shit
        timer = new CountDownTimer(upper_timer, 100) {
            TextView timeLeft = (TextView) findViewById(R.id.timer);
            public void onTick(long millisUntilFinished) {
                long value = Math.abs((millisUntilFinished)/100);
                timeLeft.setText("" + value);
            }

            public void onFinish() {
                timeLeft.setText("" + 0);
                //comment this out for testing. removes timer
                resolve(0);

            }
        }.start();

        //tapped
        if(correctAction == 1){
            img_view.setImageResource(R.drawable.tap_screen);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Tap Screen");
        }
        else if (correctAction == 2){
            img_view.setImageResource(R.drawable.swipe_left);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Left Swipe");
        }
        else if(correctAction == 3){
            img_view.setImageResource(R.drawable.swipe_right);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Right Swipe");
        }
        else if (correctAction == 4){
            img_view.setImageResource(R.drawable.swipe_up);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Up Swipe");
        }
        else if (correctAction == 5){
            img_view.setImageResource(R.drawable.swipe_down);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Down Swipe");
        }
        else if (correctAction == 6) {
            img_view.setImageResource(R.drawable.shake);
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("Shake!");
        }

    }

    // check to see if user inputted the correct action
    public void resolve(int userAction) {
        timer.cancel();

        scoretimer = new CountDownTimer(200,100) {
            @Override
            public void onTick(long millisUntilFinished) {
                star.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinish() {
                star.setVisibility(View.INVISIBLE);
            }
        };

        // congrats!
        if (userAction == correctAction) {
            upper_timer = (upper_timer > lower_timer) ? upper_timer-50 : upper_timer;
            scoretimer.start();
            score += 10;
            scoreText.setText("Score: " + score);
            mp.start();
            start();
        }
        // you suck!
        else {
            TextView text = (TextView) findViewById(R.id.action_text);
            text.setText("");
            mp1.start();
            img_view.setImageResource(R.drawable.wrong);
            failTimer = new CountDownTimer(1000,100) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    achievedHighscore();
                }
            }.start();
            //correctAction = 0; // indicates game over
        }
    }

    public void achievedHighscore() {
        List<Highscore> hsList = mDBHelper.getAllHighscores();
        int hsCount = mDBHelper.getHighscoreCount(); //total # highscores
        int minScore = (hsCount >= 10) ? hsList.get(hsCount-1).getScore() : 0; //lowest highscore on leaderboard
        if ( (hsCount >= 10) && (score > minScore) ) { //need to delete
            mDBHelper.deleteHighscore(hsList.get(hsCount-1));
        }
        if (score > minScore) {
            Intent intent = new Intent(Game.this, NewHighscore.class);
            intent.putExtra("newScore", score);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent(Game.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > Math.abs(e1.getX() - e2.getX())) {
                // up or down swipe
                if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(DEBUG_TAG, "action: up swipe");
                    resolve(4);
                }
                if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(DEBUG_TAG, "action: down swipe");
                    resolve(5);
                }
            } else {
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(DEBUG_TAG, "action: left swipe");
                    resolve(2);
                }
                // left to right swipe
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE  && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(DEBUG_TAG, "action: right swipe");
                    resolve(3);
                }
            }
        } catch(Exception e){
            Log.d(DEBUG_TAG, "Exception: " + e.getMessage());
        }

        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());

        return true;
    }

    @Override public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());

        if (correctAction == 0) {
            //achievedHighscore();
        }
        else {
            resolve(1);
        }

        return false;
    }


}
