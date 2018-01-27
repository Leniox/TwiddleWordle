package data.wordle.twittle.twittlewordle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.ldoublem.loadingviewlib.view.LVBlock;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int ANIMATION_DELAY = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                //  Create a new boolean and preference and set it to true
                boolean firstLogIn = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (firstLogIn) {

                    //  Launch app intro
                    Intent i = new Intent(MainActivity.this, Introduction.class);
                    startActivity(i);

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", false);

                    //  Apply changes
                    e.apply();
                }
            }
        });



        setContentView(R.layout.activity_splash);
        final LVBlock mLVBlock = (LVBlock) findViewById(R.id.lv_block);

        mLVBlock.setViewColor(Color.rgb(245,209,22));
        mLVBlock.setShadowColor(Color.GRAY);
        mLVBlock.startAnim(750);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                mLVBlock.stopAnim();
                // close this activity
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
                finish();

                //mProgress.setMessage("Give Us A Moment...");
                //mProgress.show();
            }
        }, ANIMATION_DELAY);







        
    }


}
