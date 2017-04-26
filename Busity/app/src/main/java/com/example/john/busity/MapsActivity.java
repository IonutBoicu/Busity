package com.example.john.busity;

import android.Manifest;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.lines;
import static com.example.john.busity.R.id.map;
import static com.example.john.busity.R.layout.popupwindow;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<LocationSettingsResult> {
    private Location startMarker;
    private float clicks = 0;
    private List<Marker> allMarkers = new ArrayList<>();
    private GoogleMap mMap;
    private String currLine = "", lastUsedLine = "", infoLine = "";
    private final String url = "http://52.41.138.245:8080/Busity";
    public static final int REQUEST_PERMISSION_LOCATION = 10;

    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Keys for storing activity state in the Bundle.
    protected static final String TAG = "MainActivity";
    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates, isGood = true;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;
    private List<String> items = new ArrayList<>();
    private List<String> stations = new ArrayList<>();
    private List<Integer> hours = new ArrayList<>();
    int RQS_GooglePlayServices=0;
    private SearchView.SearchAutoComplete searchAutoComplete;
    ResponseEntity<LocationResponse[]> currLocations;
    ResponseEntity<LineResponse[]> currLines;
    ResponseEntity<StatResponse[]> currStations;
    private HashMap<String, String> icons = new HashMap<>();
    private HashMap<String, ArrayList <Pair <Integer, Pair<Integer, String>>>> statistics = new HashMap<>();
    Handler h = new Handler();
    int delay = 5000; //milliseconds
    int k = 0;
    final Context context = this;
    AutoCompleteTextView actv1, actv2, actv3;
    TextView tv_time, tv_day;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        new LinesReq().execute();
        // Kick off the process of building the GoogleApiClient, LocationRequest, and
        // LocationSettingsRequest objects.
        //step 1
        buildGoogleApiClient();
        //step 2
        createLocationRequest();
        //step 3
        buildLocationSettingsRequest();
        //step4
        checkLocationSettings();
    }

    //step 1
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    //step 2
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    //step 3
    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }
    //step 4
    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }
    //step 5
    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {

        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    //move to step 6 in onActivityResult to check what action user has taken on settings dialog
                    status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }
    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        }
        else
        {
            goAndDetectLocation();
        }

    }

    public void goAndDetectLocation()
    {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    this
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    mRequestingLocationUpdates = true;
                }
            });
    }
    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                this
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                mRequestingLocationUpdates = false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goAndDetectLocation();
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } else
            mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public void createMarker(Double latitude, Double longitude, int icon) {
        LatLng latLng = new LatLng(latitude, longitude);

        Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(icon))));
        allMarkers.add(newMarker);
    }
    public void deleteMarkers() {
        for (Marker m : allMarkers)
            m.remove();
        allMarkers.removeAll(allMarkers);
    }
    @Override
    protected void onStart() {
        super.onStart();
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            mGoogleApiClient.connect();
        } else {
            googleAPI.getErrorDialog(this,resultCode, RQS_GooglePlayServices);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchAutoComplete = (SearchView.SearchAutoComplete)     mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, items);
        searchAutoComplete.setAdapter(adapter);

        SearchManager searchManager =
                (SearchManager) getSystemService(this.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String searchString=(String)parent.getItemAtPosition(position);

                searchAutoComplete.setText("" + searchString);
                currLine = searchAutoComplete.getText().toString();
                if (lastUsedLine.equals(""))
                    lastUsedLine = currLine;
                isGood = true;
                k = 0;
                h.postDelayed(new Runnable(){
                    public void run(){
                        //do something
                        new LocationReq().execute();
                        if (lastUsedLine.equals(currLine)) {
                            if (isGood) {
                                k++;
                                if (k % 3 == 0 && allMarkers.size() == 0)
                                    isGood = false;
                                h.postDelayed(this, delay);
                            }
                        }
                        else lastUsedLine = currLine;

                    }
                }, 1000);

            }
        });
        searchAutoComplete.setThreshold(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            // custom dialog
            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(popupwindow);
            dialog.setCanceledOnTouchOutside(true);

            // colors
            // Set title divider color
            /*int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
            View titleDivider = dialog.findViewById(titleDividerId);
            if (titleDivider != null) {
                Toast.makeText(context, "HELLO", Toast.LENGTH_SHORT).show();
                titleDivider.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                titleDivider.text
            }*/
            tv_day = (TextView) dialog.findViewById(R.id.tv_day);
            tv_time = (TextView) dialog.findViewById(R.id.tv_time);
            actv1 = (AutoCompleteTextView) dialog.findViewById(R.id.actv1);
            actv2 = (AutoCompleteTextView) dialog.findViewById(R.id.actv2);
            actv3 = (AutoCompleteTextView) dialog.findViewById(R.id.actv3);
            ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, items);
            actv1.setAdapter(adapter1);
            actv1.setThreshold(0);
            actv2.setThreshold(0);
            actv3.setThreshold(0);

            actv1.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    actv1.showDropDown();
                    return false;
                }
            });
            actv2.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    actv2.showDropDown();
                    return false;
                }
            });
            actv3.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    actv3.showDropDown();
                    return false;
                }
            });
            actv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String searchString=(String)parent.getItemAtPosition(position);
                    actv1.setText("" + searchString);
                    infoLine = actv1.getText().toString();
                    new StatReq().execute();
                }
            });

            actv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String searchString=(String)parent.getItemAtPosition(position);
                    actv2.setText("" + searchString);
                    Log.d("TAG", statistics.toString());

                    hours.clear();
                    for (Pair<Integer, Pair<Integer, String>> p : statistics.get(searchString)) {
                        hours.add(p.first);
                    }
                    ArrayAdapter<Integer> adapter = new ArrayAdapter<>(MapsActivity.this,
                            android.R.layout.simple_dropdown_item_1line, hours);
                    actv3.setAdapter(adapter);
                    actv3.showDropDown();

                }
            });

            actv3.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d("TAG", statistics.toString());
                    String searchString = actv2.getText().toString();
                    Integer searchHour=(Integer)parent.getItemAtPosition(position);
                    actv3.setText("" + searchHour);
                    for (Pair<Integer, Pair<Integer, String>> p : statistics.get(searchString)){
                        if (p.first == searchHour) {
                            int time = p.second.first;
                            String day = p.second.second;
                            if (day.equals("M_F"))
                                tv_day.setText("   Luni - Vineri");
                            else
                                tv_day.setText("   Weekend");
                            tv_time.setText("   "+time/60 + " min "+ time%60 + " sec");
                        }
                    }

                }
            });

            dialog.show();
            return true;
        } else if (id == R.id.action_search) {
            searchAutoComplete.showDropDown();
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "Connected to GoogleApiClient");
        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {

            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            }
        }
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (startMarker == null) {
            float zoomLevel = 16; //This goes up to 21
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoomLevel));
        }
        startMarker = location;
    }

    private Bitmap getMarkerBitmapFromView(@DrawableRes int resId) {
        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
        ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        markerImageView.setImageResource(resId);
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);

        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }


    private class LocationReq extends AsyncTask<Void, Void, LocationResponse[]> {
        @Override
        protected LocationResponse[] doInBackground(Void... params) {
            try {
                String currUrl = url + "/r/line?line=" + currLine;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                currLocations = restTemplate.getForEntity(currUrl, LocationResponse[].class);
                if (currLocations.hasBody())
                    return currLocations.getBody();
                else
                    return null;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(LocationResponse[] locations) {
            deleteMarkers();
            if (locations != null) {
                for (int i = 0; i < locations.length; i++) {
                    int ico = 0;
                    if (icons.get(currLine).equals("BUS"))
                        ico = R.drawable.bus;
                    else if (icons.get(currLine).equals("TRAM"))
                        ico = R.drawable.tram;
                    else
                        ico = R.drawable.troll;
                    createMarker(locations[i].getLat(), locations[i].getLongi(), ico);
                }
            } else {
                isGood = false;
            }

        }
    }

    private class LinesReq extends AsyncTask<Void, Void, LineResponse[]> {
        @Override
        protected LineResponse[] doInBackground(Void... params) {
            try {
                String currUrl = url + "/get-lines";
                Log.d("TAG", currUrl);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                currLines = restTemplate.getForEntity(currUrl, LineResponse[].class);
                if (currLines.hasBody())
                    return currLines.getBody();
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(LineResponse[] lines) {
            for (int i = 0; i < lines.length; i++) {
                items.add(lines[i].getLineNo());
                icons.put(lines[i].getLineNo(), lines[i].getType());
            }
        }
    }

    private class StatReq extends AsyncTask<Void, Void, StatResponse[]> {
        @Override
        protected StatResponse[] doInBackground(Void... params) {
            try {
                String currUrl = url + "/stat?line=" + infoLine;
                Log.d("TAG", currUrl);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                currStations = restTemplate.getForEntity(currUrl, StatResponse[].class);
                if (currStations.hasBody())
                    return currStations.getBody();

            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(StatResponse[] stats) {
            if (stats != null) {
                stations.clear();
                statistics.clear();
                hours.clear();
                if (stats == null) {
                    Toast.makeText(MapsActivity.this, "Date insuficiente pentru statistica", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < stats.length; i ++) {
                    if (!statistics.containsKey(stats[i].getStationName())) {
                        stations.add(stats[i].getStationName());
                        statistics.put(stats[i].getStationName(), new ArrayList<Pair<Integer, Pair<Integer, String>>>());
                        statistics.get(stats[i].getStationName()).add(new Pair<>(stats[i].getHour(),
                                new Pair<>(stats[i].getWaitingTime(), stats[i].getDayOfWeek())));
                    } else {
                        statistics.get(stats[i].getStationName()).add(new Pair<>(stats[i].getHour(),
                                new Pair<>(stats[i].getWaitingTime(), stats[i].getDayOfWeek())));
                    }
                }
            } else {
                stations.clear();
                statistics.clear();
            }
            Log.d("TAG",stations.toString());
            Log.d("TAG",statistics.toString());
            ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MapsActivity.this,
                    android.R.layout.simple_dropdown_item_1line, stations);
            actv2.setAdapter(adapter2);
            actv2.showDropDown();

        }
    }

}
