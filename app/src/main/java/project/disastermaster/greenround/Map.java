package project.disastermaster.greenround;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Map extends AppCompatActivity  implements OnMapReadyCallback, LocationListener {


    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    String JSONDATA =

            "[\n" +
                    "    {\n" +
                    "        \"name\": \"Thatipur Petrol Pump\",\n" +
                    "        \"latlng\": [\n" +
                    "            26.214968,\n" +
                    "            78.207110\n" +
                    "        ],\n" +
                    "        \"population\": \"123\"\n" +
                    "    },\n" +


                    "    {\n" +
                    "        \"name\": \"Chauhan piau\",\n" +
                    "        \"latlng\": [\n" +
                    "             26.215092,\n" +
                    "             78.210087\n" +
                    "        ],\n" +
                    "        \"population\": \"132\"\n" +
                    "    },\n" +

                    "    {\n" +
                    "        \"name\": \"shriram house\",\n" +
                    "        \"latlng\": [\n" +
                    "             26.212951,\n" +
                    "             78.212945\n" +
                    "        ],\n" +
                    "        \"population\": \"132\"\n" +
                    "    },\n" +

                    "    {\n" +
                    "        \"name\": \"Vikas house\",\n" +
                    "        \"latlng\": [\n" +
                    "             26.222461,\n" +
                    "             78.224380\n" +
                    "        ],\n" +
                    "        \"population\": \"132\"\n" +
                    "    },\n" +

                    "    {\n" +
                    "        \"name\": \"Baradari\",\n" +
                    "        \"latlng\": [\n" +
                    "           26.218568,\n" +
                    "            78.221754\n" +
                    "        ],\n" +
                    "        \"population\": \"123\"\n" +
                    "    }\n" +
                    "] ";


    double currentlat, currentlong, minlat, minlog;

    Marker mClosestMarker;
    float mindist;
    private GoogleMap mMap;


    private FusedLocationProviderClient fusedLocationClient;
    //qr code scanner object
    private IntentIntegrator qrScan;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean mPermissionDenied = false;

    boolean mLocationPermissionGranted = false;
    Button bt_navigate, bt_start, bt_find_ride;
    LinearLayout ll_ride;
    LocationManager locationManager;
    TextView timerTv;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        timerTv = findViewById(R.id.timertv);
        bt_navigate = (Button) findViewById(R.id.bt_navigate);
        bt_start = (Button) findViewById(R.id.bt_start);
        bt_find_ride = (Button) findViewById(R.id.bt_find);
        bt_find_ride.setVisibility(View.GONE);
        bt_navigate.setVisibility(View.GONE);
        bt_start.setVisibility(View.GONE);


        ll_ride = findViewById(R.id.ll_ride);
        ll_ride.setVisibility(View.GONE);


        //intializing scan object
        qrScan = new IntentIntegrator(this);
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            mLocationPermissionGranted = true;
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }


    // when map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;



        enableMyLocation();
        //this/*
        /*
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }

        */
        //or this
       getloc();


    }

    @Override
    public void onLocationChanged(Location location) {

        currentlat =  location.getLatitude();
        currentlong = location.getLongitude();


        LatLng trailLocation = new LatLng(currentlat, currentlong);
        mMap.addMarker(new MarkerOptions().position(trailLocation).title("Marker in Current Location"));
        //mMap.moveCamera(CameraUpdateFactory.zoomBy(50, tra));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(trailLocation));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trailLocation, 15));

        //mMap.getMaxZoomLevel(trailLocation);

        try {
            createMarkersFromJson(JSONDATA);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "location change", Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "disabled" + provider, Toast.LENGTH_SHORT).show();
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "enabled" + provider, Toast.LENGTH_SHORT).show();
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(this, "status changed", Toast.LENGTH_SHORT).show();
        Log.d("Latitude","status");
    }

    void createMarkersFromJson(String json) throws JSONException {
        // De-serialize the JSON string into an array of city objects
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            // Create a marker for each city in the JSON data.
            JSONObject jsonObj = jsonArray.getJSONObject(i);
            double lat = jsonObj.getJSONArray("latlng").getDouble(0);
            double lon = jsonObj.getJSONArray("latlng").getDouble(1);
            Marker currentMarker = mMap.addMarker(new MarkerOptions()
                    .title(jsonObj.getString("name"))
                    .snippet(Integer.toString(jsonObj.getInt("population")))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bicycle))
                    .position(new LatLng(
                            lat,
                            lon
                    ))
            );

            float[] distance = new float[1];

            Location.distanceBetween(currentlat, currentlong, lat, lon, distance);
            if(i==0) {
                mindist = distance[0];
            } else if (mindist > distance[0]) {
                mindist=distance[0];
                mClosestMarker = currentMarker;
                minlat = lat;
                minlog = lon;
            }
        }



        /////check here if distance is 0
        //if 0 then start
        // else navigate
        bt_find_ride.setVisibility(View.VISIBLE);
        //Toast.makeText(Map.this, "Closest Marker Distance: "+ mClosestMarker.getTitle() + " " + mindist, Toast.LENGTH_LONG).show();
    }

    public void find(View v){
        bt_find_ride.setVisibility(View.GONE);
        if(mindist < 120)
        {
            bt_start.setVisibility(View.VISIBLE);
        }else {
            bt_navigate.setVisibility(View.VISIBLE);
        }

    }

    public void navigate(View v){

        Uri gmmIntentUri = Uri.parse("google.navigation:q="+minlat+","+minlog+"&mode=w");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void startRide(View v){
        //
        Toast.makeText(this, "start ride scan the qr on bike", Toast.LENGTH_SHORT).show();

        //initiating the qr code scan
        qrScan.initiateScan();
    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
               // Toast.makeText(this, ""+result.getContents(), Toast.LENGTH_SHORT).show();
                fetchLock(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //fetch password
    public void fetchLock(String data){


        String password = "";
        /*
        try {
            JSONArray jsonArray = new JSONArray(JSONBIKEPASSWORD);
            for(int i = 0;i<jsonArray.length();i++){
                JSONObject obj = jsonArray.getJSONObject(i);
                if(obj.get("id").equals(data)){
                    password = obj.getString("pass");
                }
                
            }*/

        //dummy qr data = 10
        if(data.equals("10")) {
            password="7744";
        }


        if(!password.equals("")){
            bt_start.setVisibility(View.GONE);

            //start timer
            StartTime = SystemClock.uptimeMillis();
            handler = new Handler() ;

            handler.postDelayed(runnable, 0);
            //show bicycle password


            // custom dialog
            ll_ride.setVisibility(View.VISIBLE);
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog);
            dialog.setTitle("Title...");

            // set the custom dialog components - text, image and button
            TextView text = (TextView) dialog.findViewById(R.id.tv_timer);
            text.setText(password);


            dialog.setCancelable(true);

            dialog.show();




        }else{
            Toast.makeText(this, "no bike found", Toast.LENGTH_SHORT).show();
        }
        


    }

    public void endRide(View v){

        // calculate from current location
        getloc();
        try {
            createMarkersFromJson(JSONDATA);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //if marker at end position
        if(mindist<120){

             // send mail


            // create finish dialoge
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.ride_end_dialog);

            // set the custom dialog components - text, image and button
            TextView text = (TextView) dialog.findViewById(R.id.tv_head);
            text.setText("Ride finished" );

            TextView text2 = (TextView) dialog.findViewById(R.id.tv_finishtime);
            text2.setText(timerTv.getText());


            dialog.setCancelable(true);

            dialog.show();


        //set time to zero
        MillisecondTime = 0L ;
        StartTime = 0L ;
        TimeBuff = 0L ;
        UpdateTime = 0L ;
        Seconds = 0 ;
        Minutes = 0 ;
        MilliSeconds = 0 ;
        timerTv.setText("00:00");

        handler.removeCallbacks(runnable);

        ll_ride.setVisibility(View.GONE);

        if(mindist < 100)
        {
            bt_start.setVisibility(View.VISIBLE);
        }else {
            bt_navigate.setVisibility(View.VISIBLE);
        }}

        else{
            Toast.makeText(this, "Reach to nearest marker", Toast.LENGTH_SHORT).show();
        }


    }

    public void getloc() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Map.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                           currentlat =  location.getLatitude();
                           currentlong = location.getLongitude();


                            LatLng trailLocation = new LatLng(currentlat, currentlong);
                            mMap.addMarker(new MarkerOptions().position(trailLocation).title("Marker in Current Location"));
                            //mMap.moveCamera(CameraUpdateFactory.zoomBy(50, tra));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(trailLocation));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trailLocation, 15));

                            //mMap.getMaxZoomLevel(trailLocation);

                            try {
                                createMarkersFromJson(JSONDATA);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                          //  Toast.makeText(Map.this, "set location" + currentlong +"   "+ currentlat, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(Map.this, "location not set", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timerTv.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds));

            handler.postDelayed(this, 0);
        }

    };

}
