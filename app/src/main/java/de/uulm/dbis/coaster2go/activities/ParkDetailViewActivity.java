package de.uulm.dbis.coaster2go.activities;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

import de.uulm.dbis.coaster2go.R;
import de.uulm.dbis.coaster2go.data.JsonManager;
import de.uulm.dbis.coaster2go.data.Park;

public class ParkDetailViewActivity extends BaseActivity {

    private String parkId;
    private Park park;
    private boolean isFave = false;
    private Location lastLocation;

    TextView parkName, parkLocation, parkRatingAvg, parkDescription, parkDistance;
    RatingBar ratingBar;
    ImageView parkImage;
    ImageButton buttonFav, buttonMaps;
    Button buttonAttractions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_park_detail_view);

        parkName = (TextView) findViewById(R.id.park_detail_parkname);
        parkLocation = (TextView) findViewById(R.id.park_detail_location);
        parkDistance = (TextView) findViewById(R.id.park_detail_distance);
        parkRatingAvg = (TextView) findViewById(R.id.park_detail_average);
        parkDescription = (TextView) findViewById(R.id.park_detail_description);
        ratingBar = (RatingBar) findViewById(R.id.park_detail_ratingbar);
        parkImage = (ImageView) findViewById(R.id.park_detail_image);
        buttonFav = (ImageButton) findViewById(R.id.park_detail_button_favorite);
        buttonMaps = (ImageButton) findViewById(R.id.park_detail_button_maps);
        buttonAttractions = (Button) findViewById(R.id.park_detail_button_attractions);

        // enable buttons
        buttonMaps.setEnabled(false);
        buttonFav.setEnabled(false);

        //get parkID from intent (clicked list item from previous activity)
        parkId = getIntent().getStringExtra("parkId");

        //get all data from db and fill in all information
        new LoadParkAsync().execute();

        //get favorite information and set the button
        new LoadFaveAsync().execute();

        buttonAttractions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToAttractionOverview(null);
            }
        });

        buttonMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParkDetailViewActivity.this, MapViewActivity.class);
                intent.putExtra("lon", park.getLon());
                intent.putExtra("lat", park.getLat());
                intent.putExtra("name", park.getName());
                startActivity(intent);
            }
        });

        buttonFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SetFaveAsync().execute();
            }
        });


    }
    public void goToAttractionOverview(View view) {
        Intent intent = new Intent(this, AttractionOverviewActivity.class);
        intent.putExtra("parkId", parkId);
        startActivity(intent);
    }

    public void showParkRatings(View view) {
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("reviewedId", parkId);
        intent.putExtra("reviewedName", park.getName());
        startActivity(intent);
    }


    public class LoadParkAsync extends AsyncTask<Void, Void, Park> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Park doInBackground(Void... params) {
            //return new AzureDBManager(ParkDetailViewActivity.this).getParkById(parkId);
            //Load offline data because it got updated right before in the Park Overview:
            return new JsonManager(ParkDetailViewActivity.this).getParkById(parkId);
        }

        @Override
        protected void onPostExecute(Park park2) {
            if (park2 == null) {
                Log.e("", "LoadParkAsync.onPostExecute: park was null!");
                Snackbar.make(findViewById(R.id.coordinatorLayout_ParkDetailview),
                        "Park konnte nicht geladen werden", Snackbar.LENGTH_SHORT);
            } else {
                park = park2;
                if (park2.getImage() == null || park2.getImage().isEmpty()) {
                    Picasso.with(ParkDetailViewActivity.this)
                            .load(R.mipmap.ic_launcher).into(parkImage);
                } else {
                    Picasso.with(ParkDetailViewActivity.this).load(park2.getImage()).into(parkImage);
                }
                parkName.setText(park2.getName());
                parkName.setSelected(true);
                ratingBar.setRating((float) park2.getAverageReview());
                DecimalFormat df = new DecimalFormat("#.#");
                parkRatingAvg.setText(df.format(park2.getAverageReview()));
                parkLocation.setText(park2.getLocation());
                parkDescription.setText(park2.getDescription());

                Location parkLocationLatLng = new Location("");
                parkLocationLatLng.setLatitude(park.getLat());
                parkLocationLatLng.setLongitude(park.getLon());
                if (lastLocation == null) {
                    //parkDistance.setVisibility(View.INVISIBLE);
                    parkDistance.setText("TODO get location");
                } else {
                    parkDistance.setVisibility(View.VISIBLE);
                    Location parkLoc = new Location("");
                    parkLoc.setLongitude(park.getLon());
                    parkLoc.setLatitude(park.getLat());

                    float distance = lastLocation.distanceTo(parkLoc);

                    parkDistance.setText(buildDistanceString(distance));

                }


                // enable buttons
                buttonMaps.setEnabled(true);
                buttonFav.setEnabled(true);
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    public class LoadFaveAsync extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return new JsonManager(ParkDetailViewActivity.this).isParkFavorite(parkId);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(b){
                buttonFav.setImageResource(R.drawable.ic_favorite_red_24dp);
                isFave = true;
            }else{
                buttonFav.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                isFave = false;
            }
        }
    }

    public class SetFaveAsync extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            if(isFave){
                return new JsonManager(ParkDetailViewActivity.this).deleteParkFromFavorites(parkId);
            }else{
                return new JsonManager(ParkDetailViewActivity.this).putIntoParkFavorites(parkId);
            }

        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(b){
                if(!isFave){
                    buttonFav.setImageResource(R.drawable.ic_favorite_red_24dp);
                    isFave = true;
                }else{
                    buttonFav.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    isFave = false;
                }
            }else{
                Log.e("", "Put Park into favorites didn't work!");
            }
        }
    }

    /**
     * builds an appropriate string based on the distance
     * @param distanceMeters the distance in meters
     * @return a string showing the distance in m or km
     */
    private String buildDistanceString(float distanceMeters) {
        if (distanceMeters > 1000) {
            int distanceKm = Math.round(distanceMeters/1000);
            return distanceKm + " km";
        } else {
            return Math.round(distanceMeters) + " m";
        }
    }

}


