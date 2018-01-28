package data.wordle.twittle.twittlewordle;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.geonames.PostalCode;
import org.geonames.PostalCodeSearchCriteria;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.WebService;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EmotionOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.kennycason.kumo.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveStartedListener, NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnMarkerClickListener
{
    protected MapView mapView;
    protected GoogleMap myMap;
    private Location location;
    private DrawerLayout di;
    boolean isBearerComputed = false;
    String [] bearerToken = new String[1];
    private int locationPermissionCode = 5;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 10;
    LinkedHashSet<LatLng> nearByCodes = new LinkedHashSet<>();
    ArrayList<AnalysisResults> individualTweetResults = new ArrayList<>();
    HashMap<LatLng, AnalysisResults> analysisRegionHashMap = new HashMap<>();
    HashMap<LatLng, ArrayList<AnalysisResults>> analysisIndividualTweetMap = new HashMap<>();
    long startTime = 0;
    ActionBarDrawerToggle toggle;
    boolean didInitialZoom = false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_drawer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");



        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

         toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimary));
        }






        startTime = System.currentTimeMillis();
        location = getLastKnownLocation();
        mapView = (MapView) findViewById(R.id.map1);
        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(this);


        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                //call twitter again with a progress bar.
                callPlaceAutocompleteActivityIntent();



            }

            @Override
            public void onError(Status status) {

            }
        });

    }






    private void callPlaceAutocompleteActivityIntent() {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
//PLACE_AUTOCOMPLETE_REQUEST_CODE is integer for request code
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(this);
        myMap = googleMap;
        myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (didInitialZoom == false)
        {
            didInitialZoom = true;
            try {
                zoomToLocation();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (LatLng somePlace : nearByCodes)
        {
            myMap.addMarker(new MarkerOptions()
                    .position(somePlace)
                    .title("Inspect For Detail"));


        }




    }

    private void zoomToLocation() throws InterruptedException {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {




            if (location != null) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                        .zoom(17)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                myMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude())));
                boolean didSucceed = findNearByLocations(location);
                if (didSucceed)
                {

                    mapView.getMapAsync(this);

                    callTwitterAPI();










//                                                callTwitterAPI();

                    //call this in another thread?
//                    Thread testingThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            callTwitterAPI();
//
//                        }
//                    });
                }



            }
            else {
                requestPermissions(
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,},
                        locationPermissionCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                zoomToLocation();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //autocompleteFragment.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                //CALL TWITTER API HERE.
                Log.i("Place", "Place:" + place.toString());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i("Place", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }


    protected Location getLastKnownLocation() {

        Location bestLocation = null;
        if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            LocationManager mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

            mLocationManager = (LocationManager)this.getApplicationContext().getSystemService(LOCATION_SERVICE);

            List<String> providers = mLocationManager.getProviders(true);
            for (String provider : providers) {
                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }
        }
        return bestLocation;


    }

    @Override
    public void onMapClick(LatLng latLng) {

        //toggle the search function at the top.
        //http://api.geonames.org/findNearbyPostalCodes?lat=38.907609&lng=-77.072258&username=leniox77&maxRows=150&radius=15
    }

    @Override
    public void onCameraMoveStarted(int i) {
        //toggle refresh call for twitter clusters and start a slow camera movement to display the data as it comes in instead of an idle map
        //get current location and call this:
        //http://api.geonames.org/findNearbyPostalCodes?lat=38.907609&lng=-77.072258&username=leniox77&maxRows=150&radius=15
    }

    public boolean findNearByLocations(Location myLocation)
    {
        WebService.setUserName("leniox77");
        final PostalCodeSearchCriteria postalCodeSearchCriteria = new PostalCodeSearchCriteria();
        try
        {
            //reverse geocoding
            postalCodeSearchCriteria.setLatitude(myLocation.getLatitude());
            postalCodeSearchCriteria.setLongitude(myLocation.getLongitude());
            postalCodeSearchCriteria.setMaxRows(15);
            postalCodeSearchCriteria.setRadius(10.0);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        List postalCodes = WebService.findNearbyPostalCodes(postalCodeSearchCriteria);
                        processFinish(postalCodes);

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            });
            thread.start();
            thread.join();
            return true;





        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }



    }
    public void processFinish(List postalCodesXML)
    {

        System.out.println("HI MY NAME IS GREG");

       ArrayList<PostalCode> list = (ArrayList<PostalCode>) postalCodesXML;
        for (int i = 0; i < list.size(); i++)
        {
            PostalCode someCode =  list.get(i);

            Double lat = (Double) someCode.getLatitude();
            Double longit = (Double) someCode.getLongitude();
            nearByCodes.add(new LatLng(lat, longit));

        }


    }
    public void callTwitterAPI()
    {
        //make a broadcast listner that will change a boolean whenver we call call twitter and whenever we end call twitter. Once
        //we call it, we set progress dialogs, Once we end it, we show the data.

        final HashMap<LatLng, ArrayList<String>> totalTweets = new HashMap<>();
        if (isBearerComputed == false)
        {
            final String[] testing = new String[1];
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    testing[0] = get_bearer();
                    bearerToken = testing;
                }
            });
            th.start();
            try {
                th.join();
                isBearerComputed = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


        final String bear_token = bearerToken[0];
        ArrayList<Thread> threadList = new ArrayList<>();
        final Iterator<LatLng> iter = nearByCodes.iterator();
        while (iter.hasNext())
        {
            final LatLng myPosition = iter.next();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> tweets = makeActualCall(bear_token, myPosition.latitude, myPosition.longitude);
                    totalTweets.put(myPosition, tweets);
                }
            });
            threadList.add(thread);
            thread.start();

        }
        for (Thread t : threadList)
        {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(totalTweets);
        //and watson call here.
        //everyhting should be done now.
        //NOW DO A WORD COUNT HERE.
        //
        //# of threads = size of nearByCodes. Wait until all finish to proceed. I should be doing this during splash.
        threadList.clear();
         int counter = 0;
        for (final LatLng pos : totalTweets.keySet())
        {
            final ArrayList<String> list = totalTweets.get(pos);
            counter++;
            System.out.println(counter);

            Thread someThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    String listString = String.join(", ", list);
                    analyzeSentimentGroupTweets(listString, pos);


                }
            });
            threadList.add(someThread);
            someThread.start();
        }


//        for (final LatLng pos : totalTweets.keySet())
//        {
//            final ArrayList<String> list = totalTweets.get(pos);
//
//            Thread someThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
////                    String listString = String.join(", ", list);
//                    analyzeSentimentEachTweet(list, pos);
//                }
//            });
//            threadList.add(someThread);
//            someThread.start();
//
//
//        }

        for (Thread t : threadList)
        {
            try {
                t.join();
                counter--;
                System.out.println(counter);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        System.out.println(analysisRegionHashMap);
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;
        long seconds  = TimeUnit.MILLISECONDS.toSeconds(total) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(total));
        System.out.println("total time: " + seconds + "seconds");
        Log.d("MyTagGoesHere", "This is my log message at the debug level here");
        Log.e("MyTagGoesHere", "This is my log message at the error level here");
        Log.d("MyTagGoesHere", new Long(seconds).toString());



    }

    //reason for analyzing individually is that IBM Watson is going to analyze text sentiment based on individual tweets instead of
    //a  large chunk of nonsequitor knowledge from various people. Could give better accurate predictions on sentiment if I
    //take the average of all the tweets. Something to test.
    public void analyzeSentimentEachTweet(ArrayList<String> listOfStrings, final LatLng pos)
    {
        final NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
                NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,
                "70f69755-97b1-4f37-8656-fc05ea213e95",
                "UzjKdRUdwqX0"
        );
        ArrayList<Thread> threadList = new ArrayList<>();

        for (final String s : listOfStrings)
        {

            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    CategoriesOptions categories = new CategoriesOptions();

                    EmotionOptions emotion= new EmotionOptions.Builder().build();

                    EntitiesOptions entitiesOptions = new EntitiesOptions.Builder()
                            .emotion(true)
                            .sentiment(true)
                            .build();

                    KeywordsOptions keywordsOptions = new KeywordsOptions.Builder()
                            .emotion(true)
                            .sentiment(true)
                            .build();


                    Features features = new Features.Builder()
                            .categories(categories)
                            .emotion(emotion)
                            .entities(entitiesOptions)
                            .keywords(keywordsOptions)
                            .build();

                    AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                            .text(s)
                            .features(features)
                            .build();

                    AnalysisResults response = service
                            .analyze(parameters)
                            .execute();


                    ArrayList<AnalysisResults> results = new ArrayList<>();
                    if (analysisIndividualTweetMap.get(pos) != null)
                    {
                        results = analysisIndividualTweetMap.get(pos);
                    }
                    //after each indivudal tweet within a position, add it to its list and add update the map.
                    results.add(response);
                    analysisIndividualTweetMap.put(pos, results);


                }
            });
            threadList.add(th);
            th.start();


        }

        for (Thread t : threadList)
        {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(analysisIndividualTweetMap.toString());






    }

    public void analyzeSentimentGroupTweets(String s, LatLng position)
    {

        //take care of duplicate entries of areas
        if (analysisRegionHashMap.keySet().contains(position))
        {
            return;
        }

        final NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
                NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,
                "70f69755-97b1-4f37-8656-fc05ea213e95",
                "UzjKdRUdwqX0"
        );

        CategoriesOptions categories = new CategoriesOptions();

        EmotionOptions emotion= new EmotionOptions.Builder().build();

        EntitiesOptions entitiesOptions = new EntitiesOptions.Builder()
                .emotion(true)
                .sentiment(true)
                .build();

        KeywordsOptions keywordsOptions = new KeywordsOptions.Builder()
                .emotion(true)
                .sentiment(true)
                .build();


        Features features = new Features.Builder()
                .categories(categories)
                .emotion(emotion)
                .entities(entitiesOptions)
                .keywords(keywordsOptions)
                .build();

        AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                .text(s)
                .features(features)
                .build();

        AnalysisResults response = service
                .analyze(parameters)
                .execute();
        analysisRegionHashMap.put(position, response);

    }

    public void countWordsHashMap()
    {
//        WordFrequency wordFrequency = new WordFrequency("dog",3);
//        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);

    }

    public String get_bearer()
    {
        String authString = "2uSBRzRORaM2qBguhwPug7LSe:T42SCO6DZ084AKvcnbC5N7cdlQ60UW7SuSfsU4jCSheX6jpQVh";
        String basicAuth = "Basic " + Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
//        byte [] array  = java.util.Base64.getEncoder().encode(authString.getBytes());
//        String testing = array.;


        String postBody = "grant_type=client_credentials";

        HttpURLConnection urlConnection = null;
        URL url;
        try {
            url = new URL("https://api.twitter.com/oauth2/token?grant_type=client_credentials");
            urlConnection = (HttpURLConnection) url.openConnection();
            String testingThisthing = String.valueOf(postBody.getBytes().length);

            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            urlConnection.setRequestProperty("Content-Length", String.valueOf(postBody.getBytes().length));
//            urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            urlConnection.setRequestProperty("User-Agent", "SomethingMeaningful");
//            urlConnection.setFixedLengthStreamingMode(postBody.getBytes().length);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);

            OutputStream output = urlConnection.getOutputStream();
            output.write(postBody.getBytes("UTF-8"));
            output.flush();
            output.close();

            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = urlConnection.getInputStream();
//                ByteArrayOutputStream result = new ByteArrayOutputStream();
//                byte[] buffer = new byte[1024];
//                int length;
//                while ((length = is.read(buffer)) != -1) {
//                    result.write(buffer, 0, length);
//                }
//                String testing = result.toString();

                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                JSONObject j = new JSONObject(responseStrBuilder.toString());
                String accessToken = (String) j.get("access_token");
                is.close();
//                result.close();
                return accessToken;

            } else {
                Log.e("Twitter HTTP Error", urlConnection.getResponseCode() + urlConnection.getResponseMessage());
                System.out.println("twitter error: " + urlConnection.getResponseMessage());
                InputStream is = urlConnection.getErrorStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                Log.e("Twitter Auth Error", result.toString());
                is.close();
                result.close();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public ArrayList<String> makeActualCall(String access_token, Double lati, Double longi)
    {
        String lat = Double.toString(lati);
        String lon = Double.toString(longi);
        String max_range = Integer.toString(10);
        String postBody = "https://api.twitter.com/1.1/search/tweets.json?q=&geocode=" + lat + "," + lon + "," + max_range + "mi&count=50";


        HttpURLConnection urlConnection = null;
        URL url;
        try {
            url = new URL(postBody);
            String clength = String.valueOf(postBody.getBytes().length);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Bearer " + access_token);
//            urlConnection.setRequestProperty("Content-Length", String.valueOf(postBody.getBytes().length));
//            urlConnection.setFixedLengthStreamingMode(postBody.getBytes().length);
            urlConnection.setRequestMethod("GET");
//            urlConnection.setDoInput(true);

            InputStream output = urlConnection.getInputStream();

            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = urlConnection.getInputStream();
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                JSONObject raw_data = new JSONObject(responseStrBuilder.toString());
                JSONArray statuses = raw_data.getJSONArray("statuses");

                ArrayList<String> tweets = new ArrayList<>();

                for (int i = 0; i < statuses.length(); i++) {
                    JSONObject status = statuses.getJSONObject(i);
                    tweets.add(status.getString("text"));
                }
                is.close();
                return tweets;
            } else {
                Log.e("Twitter HTTP Error", urlConnection.getResponseCode() + urlConnection.getResponseMessage());
                InputStream is = urlConnection.getErrorStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                Log.e("Twitter Feed Error", result.toString());
                is.close();
                result.close();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Intent intent;
        if (id == R.id.sign_up) {
            intent = new Intent(this, TwitterSignUp.class);





            startActivity(intent);

        } else if (id == R.id.about) {
            startActivity(new Intent(this, About.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        IndividualTwitterComp individualTwitterComp = new IndividualTwitterComp();
        individualTwitterComp.analysisRegionHashMap = this.analysisRegionHashMap;
        individualTwitterComp.thePosiiton = marker.getPosition();
        Intent intent = new Intent(MapActivity.this, individualTwitterComp.getClass());
        startActivity(intent);

        return false;
    }
}
