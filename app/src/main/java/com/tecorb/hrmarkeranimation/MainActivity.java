package com.tecorb.hrmarkeranimation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tecorb.hrmovecarmarkeranimation.AnimationClass.HRMarkerAnimation;
import com.tecorb.hrmovecarmarkeranimation.CallBacks.UpdateLocationCallBack;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "LocationActivity";
    private static final long INTERVAL = 2000; //1.5 min
    private static final long FASTEST_INTERVAL = 1000; //1.5 min
    private static final long DISPLACEMENT = 5; //5 meter
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location oldLocation;
    private Context context;
    private int markerCount=0;
    private SupportMapFragment mapFragment;
    private static final int REQUEST_LOCATION = 0;
    private boolean isFirstTime = true;
    private GoogleMap mMap;
    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context=this;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        createLocationRequest();
        markerCount = 0;


        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        markerCount = 0;
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " +
                mGoogleApiClient.isConnected());
        startLocationUpdates();
        displayLocation();
    }

    protected void startLocationUpdates() {
        try {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG, "Location update started ..............: ");
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location currentL) {
        mLastLocation = currentL;
        displayLocation();
    }

    protected void stopLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.d(TAG, "Location update stopped .......................");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    public void addMarker(GoogleMap googleMap, double lat, double lon) {

        try {

            if (markerCount == 1) {
                if (oldLocation != null) {
                    new HRMarkerAnimation(googleMap,1000, new UpdateLocationCallBack() {
                        @Override
                        public void onUpdatedLocation(Location updatedLocation) {
                            oldLocation = updatedLocation;
                        }
                    }).animateMarker(mLastLocation, oldLocation, marker);
                } else {
                    oldLocation = mLastLocation;
                }
            } else if (markerCount == 0) {
                if (marker != null) {
                    marker.remove();
                }
                mMap = googleMap;

                LatLng latLng = new LatLng(lat, lon);

                marker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                mMap.setPadding(2000, 4000, 2000, 4000);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));

                /*################### Set Marker Count to 1 after first marker is created ###################*/

                markerCount = 1;

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayLocation() {
        try {

            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                // Check Permissions Now
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
            } else {


                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mLastLocation != null && mLastLocation.getLongitude() != 0.0 && mLastLocation.getLongitude() != 0.0) {

                    if (mMap != null) {
                        addMarker(mMap, mLastLocation.getLatitude(), mLastLocation.getLongitude());

                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
