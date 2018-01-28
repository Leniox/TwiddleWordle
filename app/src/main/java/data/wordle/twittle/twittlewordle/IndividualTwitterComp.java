package data.wordle.twittle.twittlewordle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;

import java.util.HashMap;

public class IndividualTwitterComp extends AppCompatActivity {

    HashMap<LatLng, AnalysisResults> analysisRegionHashMap = new HashMap<>();
    LatLng thePosiiton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_twitter_comp);
    }
}
