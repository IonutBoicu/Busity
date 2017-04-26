package com.example.john.driverbusity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.android.gms.maps.model.LatLng;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener,ResultCallback<LocationSettingsResult>  {

    class Message {
        public static final String INEXISTENT_LINE = "LINE DOES NOT EXIST";
        public static final String UNREGISTERED_VEHICLE = "VEHICLE NOT REGISTERED";
        public static final String SUCCESS = "Success";
        public static final String ERROR = "Error";
        public static final String CONNECTED = "Deconectare server";
        public static final String DISCONNECTED = "Conectare server";
    }
    int RQS_GooglePlayServices=0;
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
    // Labels.
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    private Button btn;
    private EditText et_line, et_matr;
    private final String url = "http://52.41.138.245:8080/Busity", dateformat = "MMddyyyyHHmmss", timezone = "GMT+3";
    private String line = "", matr = "";
    private ResponseMessage rm;
    private boolean unregistered = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        et_line = (EditText) findViewById(R.id.line);
        et_matr = (EditText) findViewById(R.id.matr);
        et_line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et_line.setText("");
            }
        });

        et_matr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et_line.setText("");
            }
        });

        btn = (Button) findViewById(R.id.send_location);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                line = et_line.getText().toString();
                matr = et_matr.getText().toString();
                if (btn.getText().equals(Message.DISCONNECTED)) {
                    if (!line.equals("") && !matr.equals("")) {
                        new RegisterServer().execute();
                    } else {
                        Toast.makeText(MapsActivity.this, "Introduceti date valide in ambele campuri", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    unregistered = true;
                    new UnRegisterServer().execute();
                }
            }
        });

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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    //step 2
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
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
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        else
            goAndDetectLocation();
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
                    //     setButtonsEnabledState();
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
                //   setButtonsEnabledState();
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
            //  Toast.makeText(FusedLocationWithSettingsDialog.this, "location was already on so detecting location now", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }

    @Override
    public void onDestroy() {
        unregistered = true;
        super.onDestroy();
    }

    private class RegisterServer extends AsyncTask<Void, Void, ResponseMessage> {
        @Override
        protected ResponseMessage doInBackground(Void... params) {
            try {
                String currUrl = url + "/register?line=" + line + "&registr=" + matr;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                Log.d("TAG", currUrl);
                return restTemplate.getForObject(currUrl, ResponseMessage.class);
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ResponseMessage msg) {
            Log.d("TAG", msg.toString());
            rm = new ResponseMessage(msg.getType(), msg.getMessage());

            if (rm.getType().equals(Message.ERROR)) {
                if (rm.getType().equals(Message.INEXISTENT_LINE))
                    Toast.makeText(MapsActivity.this, "Linie inexistenta", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MapsActivity.this, "Nr Matricol inexistent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MapsActivity.this, Message.SUCCESS, Toast.LENGTH_SHORT).show();
                et_line.setEnabled(false);
                et_matr.setEnabled(false);
                btn.setText(Message.CONNECTED);

                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        while (true) {
                            if (unregistered)
                                return null;
                            DateFormat df = new SimpleDateFormat(dateformat);
                            df.setTimeZone(TimeZone.getTimeZone(timezone));
                            String date = df.format(Calendar.getInstance().getTime());
                            String currUrl = url + "/update-location?" + "registr=" + matr + "&lat="
                                    + mCurrentLocation.getLatitude() + "&long=" + mCurrentLocation.getLongitude() + "&date=" + date;
                            RestTemplate restTemplate = new RestTemplate();
                            Log.d("TAG", currUrl);
                            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                            restTemplate.getForObject(currUrl, Void.class);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.execute();
            }
        }
    }
    private class UnRegisterServer extends AsyncTask<Void, Void, ResponseMessage> {
        @Override
        protected ResponseMessage doInBackground(Void... params) {
            try {
                String currUrl = url + "/unregister?registr=" + matr;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                Log.d("TAG", currUrl);
                return restTemplate.getForObject(currUrl, ResponseMessage.class);
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ResponseMessage msg) {
            Log.d("TAG", msg.toString());
            rm = new ResponseMessage(msg.getType(), msg.getMessage());
            if (rm.getType().equals(Message.ERROR))
                Toast.makeText(MapsActivity.this, "Nr matricol inexistent", Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(MapsActivity.this, Message.SUCCESS, Toast.LENGTH_SHORT).show();
                btn.setText(Message.DISCONNECTED);
                et_line.setEnabled(true);
                et_matr.setEnabled(true);
            }
        }
    }
}
