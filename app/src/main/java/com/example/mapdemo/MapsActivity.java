package com.example.mapdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Switch;
import android.widget.Toast;

import com.example.mapdemo.adapter.CustomInfoWindowAdapter;
import com.example.mapdemo.model.InfoWindowData;
import com.example.mapdemo.model.MyItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    @BindView(R.id.switchForActionBar)
    Switch switchForActionBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private GoogleMap mMap;
    private Location mLastLocation;
    Marker mCurrLocationMarker;

    Marker centerPoint;
    Marker clusterMarker;

    //private TextView locationTv;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    ArrayList<LatLng> markerPoints = new ArrayList<>();
    private LatLng dest;
    private LatLng origin;
    private List<LatLngModel> tempList;
    ArrayList<LatLng> points;

    JSONObject jObject;

    DirectionsJSONParser parser = new DirectionsJSONParser();

    private ClusterManager<MyItem> mClusterManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // locationTv = findViewById(R.id.location);
        // we add permissions we need to request location of the users

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[0]), ALL_PERMISSIONS_RESULT);
            }
        }

        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkPlayServices()) {
            Toast.makeText(this, "You need to install Google Play Services to use the App properly", Toast.LENGTH_SHORT).show();
            // locationTv.setText("You need to install Google Play Services to use the App properly");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (mLastLocation != null) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setNumUpdates(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null) {
            if (mMap != null) {
                mMap.clear();
            }
            mLastLocation = location;
            if (mCurrLocationMarker != null) {
                mCurrLocationMarker.remove();
            }
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            LatLng latLng = new LatLng(latitude, longitude);

            showLocation(latLng);

            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng1) {
                    MapsActivity.this.showLocation(latLng1);

                    if (markerPoints.size() > 1) {
                        markerPoints.clear();
                        mMap.clear();
                    }

                    // Adding new item to the ArrayList
                    markerPoints.add(latLng1);

                    // Creating MarkerOptions
                    MarkerOptions options = new MarkerOptions();

                    // Setting the position of the marker
                    options.position(latLng1);

                    if (markerPoints.size() == 1) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else if (markerPoints.size() == 2) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    // Add new marker to the Google Map Android API V2
                    mMap.addMarker(options);

                    // Checks, whether start and end locations are captured
                    if (markerPoints.size() >= 2) {
                        origin = markerPoints.get(0);
                        dest = markerPoints.get(1);

                        // Getting URL to the Google Directions API
                        String url = MapsActivity.this.getDirectionsUrl(origin, dest);

                        DownloadTask downloadTask = new DownloadTask();

                        // Start downloading json data from Google Directions API
                        downloadTask.execute(url);

                   /* for (int i = 0; i < tempList.size(); i++) {
                        LatLng latLng2 = new LatLng(tempList.get(i).getLat(), tempList.get(i).getLng());
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng2);
                        mMap.addMarker(markerOptions);
                    }*/
                    }
                }
            });
            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    if (centerPoint != null) {
                        centerPoint.remove();
                    }
                    LatLng myLatLng = mMap.getCameraPosition().target;

                    Log.e("MyLatLng", String.valueOf(myLatLng.latitude));
                    //showLocation(myLatLng);
                    MarkerOptions myOptions = new MarkerOptions();
                    myOptions.position(myLatLng);
                    myOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    centerPoint = mMap.addMarker(myOptions);
                }
            });
            mMap.getUiSettings().setZoomControlsEnabled(true);
            setUpClusterer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MapsActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", (dialogInterface, i) -> requestPermissions(permissionsRejected.
                                            toArray(new String[0]), ALL_PERMISSIONS_RESULT)).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                        startLocationUpdates();
                    }
                }

                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }

        switchForActionBar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                MapsActivity.this, R.raw.style_json));

                if (!success) {
                    Log.e("Map", "Style parsing failed.");
                }
            } else {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                MapsActivity.this, R.raw.default_json));

                if (!success) {
                    Log.e("Map", "Style parsing failed.");
                }
            }
        });

    }

    public void showLocation(LatLng latLng) {
        Geocoder geocoder;
        List<Address> addresses = new ArrayList<>();
        geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses == null) {
            return;
        }

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String street = address.substring(0, address.indexOf(','));

        //Place current location marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.location_pin);
        //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerOptions.icon(icon);
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        InfoWindowData info = new InfoWindowData(street, city, state, country, postalCode);

        Log.e("TAG", street);

        CustomInfoWindowAdapter customInfoWindow = new CustomInfoWindowAdapter(MapsActivity.this);
        mMap.setInfoWindowAdapter(customInfoWindow);

        mCurrLocationMarker = mMap.addMarker(markerOptions);
        mCurrLocationMarker.setTag(info);

        mCurrLocationMarker.showInfoWindow();
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {


            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);


                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {

            PolylineOptions lineOptions = null;
            //MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);

                    tempList = new ArrayList<>();

                    tempList.add(new LatLngModel(lat, lng));

                    /*for (int p = 0; p < tempList.size(); p++) {
                        Log.e("tempList", String.valueOf(tempList.get(p).getLat()));
                        LatLng latLng2 = new LatLng(tempList.get(p).getLat(), tempList.get(p).getLng());
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng2);
                        mMap.addMarker(markerOptions);
                    }*/

                    points.add(position);
                }


                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

            distance = parser.calculateDistance(jObject);
            duration = parser.calculateDuration(jObject);
            Log.e("DISTANCE", distance);
            Toast.makeText(MapsActivity.this, "Distance : " + distance + " Time : " + duration, Toast.LENGTH_SHORT).show();

// Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);

            zoomRoute(mMap, points);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.location_pin);
            setAnimation(mMap, points, icon);
        }
    }

    public void zoomRoute(GoogleMap googleMap, List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 100;

        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));

        //googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
    }

    public static void setAnimation(GoogleMap myMap, final List<LatLng> directionPoint, final Bitmap bitmap) {
        Marker marker = myMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .position(directionPoint.get(0))
                .flat(true));

        //myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(directionPoint.get(0), 10));

        animateMarker(myMap, marker, directionPoint, false);
    }

    private static void animateMarker(GoogleMap myMap, final Marker marker, final List<LatLng> directionPoint, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myMap.getProjection();
        final long duration = 300000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                if (i < directionPoint.size())
                    marker.setPosition(directionPoint.get(i));
                i++;


                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
       /* String sensor = "sensor=false";
        String mode = "mode=driving";*/
        // Building the parameters to the web service
        //String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        String parameters = str_origin + "&" + str_dest + "&key=AIzaSyCDS1-W3V2Q4H9V8RNzYDHg7Y_L5EwSXA8";
        // Output format
        String output = "json";

        // Building the url to the web service
        //String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        //String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "mode=driving&"
                + "transit_routing_preference=less_driving&"
                + "origin=" + origin.latitude + "," + origin.longitude + "&"
                + str_dest + "&"
                + "key=AIzaSyCDS1-W3V2Q4H9V8RNzYDHg7Y_L5EwSXA8";

        Log.e("OutPut-->", url);
        //Toast.makeText(MapsActivity.this, url, Toast.LENGTH_SHORT).show();
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return data;
    }


    private void setUpClusterer() {
        // Position the map.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.103186, 75.126446), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this, mMap);

        // Point the map's listeners at the listeners implemented by the cluster manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    private void addItems() {

        // Set some lat/lng coordinates to start with.
        double lat = 27.203186;
        double lng = 75.126446;

        // Add ten cluster items in close proximity, for purposes of this example.
        for (int i = 0; i < 10; i++) {
            double offset = i / 60d;
            lat = lat + offset;
            lng = lng + offset;
            MyItem offsetItem = new MyItem(lat, lng);
            mClusterManager.addItem(offsetItem);
        }
    }
}