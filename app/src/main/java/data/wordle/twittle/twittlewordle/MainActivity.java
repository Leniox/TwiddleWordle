package data.wordle.twittle.twittlewordle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.ldoublem.loadingviewlib.view.LVBlock;

import java.util.LinkedHashSet;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int ANIMATION_DELAY = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences getPrefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor e = getPrefs.edit();


                //  Initialize SharedPreferences


                //  Create a new boolean and preference and set it to true
                boolean firstLogIn = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (firstLogIn )
                {
                    //  Launch app intro
                    Intent i = new Intent(MainActivity.this, Introduction.class);
                    startActivity(i);

                    //  Make a new preferences editor

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", false);

                    //  Apply changes
                    e.apply();
                }
                else
                {



                    setContentView(R.layout.activity_splash);
                    final LVBlock mLVBlock = (LVBlock) findViewById(R.id.lv_block);
                    final MapActivity mapActivity = new MapActivity();
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


                    //I can't seem to find a way to do call these methods while doing loading without race conditions or passing info problens
//                    Location myLocation = mapActivity.getLastKnownLocation(this, this);
//                    LinkedHashSet<LatLng> results = mapActivity.findNearByLocations(myLocation);
//
//                    mapActivity.callTwitterAPI();




                }

    }

    public static Bitmap mergeImages(Bitmap bottomImage, Bitmap topImage) {
        final Bitmap output = Bitmap.createBitmap(bottomImage.getWidth(), bottomImage
                .getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        canvas.drawBitmap(bottomImage, 0, 0, paint);
        canvas.drawBitmap(topImage, 0, 0, paint);

        return output;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Bitmap icon1 = BitmapFactory.decodeResource(getResources(),R.drawable.circle_data);
        Bitmap icon2 = BitmapFactory.decodeResource(getResources(),R.drawable.twitter_logo);

        mergeImages(icon1, icon2);




    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//
//
//    }
//    @Override
//    public void onPause() {
//        super.onPause();
//        final LVBlock mLVBlock = (LVBlock) findViewById(R.id.lv_block);
//        mLVBlock.stopAnim();
//
//    }
//
//
//    @Override
//    public void onStop() {
//        super.onStop();
//
//    }


}
