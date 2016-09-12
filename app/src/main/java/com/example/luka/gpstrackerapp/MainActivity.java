package com.example.luka.gpstrackerapp;

import android.os.Bundle;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

import static java.lang.Math.abs;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    SupportMapFragment sMapFragment;
    String debuggingString = "DEBUG";
    Socket socket = null;
    String host = "10.10.0.97";
    int PORT = 20152;
    ObjectInputStream in;

    ArrayList<Pair> pairs = new ArrayList<Pair>();
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ArrayList<LatLng> positions = new ArrayList<LatLng>();

    int clientCnt = 0;
    String name;
    Double latitude, longitude;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sMapFragment = SupportMapFragment.newInstance();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        final Menu menu = navigationView.getMenu();

        navigationView.setNavigationItemSelectedListener(this);

        sMapFragment.getMapAsync(this);

        android.support.v4.app.FragmentManager sFm = getSupportFragmentManager();

        if (!sMapFragment.isAdded())
            sFm.beginTransaction().add(R.id.map, sMapFragment).commit();
        else
            sFm.beginTransaction().show(sMapFragment).commit();

        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e(debuggingString, "Attempting to connect to server");
                    socket = new Socket(host, PORT);
                    boolean firstpass = true;
                    in = new ObjectInputStream(socket.getInputStream());
                    while (true) {
                        clientCnt = (Integer) in.readObject();
                        Log.e(debuggingString, String.valueOf(clientCnt));
                        if (firstpass == true) { //popunjavanje pairs liste na prvom prolazu
                            for (int i = 0; i < clientCnt; i++) {
                                name = (String) in.readObject();
                                latitude = (Double) in.readObject();
                                longitude = (Double) in.readObject();
                                pairs.add(new Pair(name, latitude, longitude));
                                //menu.add(pairs.get(i).getName());

                                menu.add(0, i, i, pairs.get(i).getName());

                                menu.getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){

                                    @Override
                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                        int id = menuItem.getItemId();
                                        showClickedMarkerTitle(id);
                                        return false;
                                    }
                                });

                                /*Log.e(debuggingString, String.valueOf(pairs.get(i).getName()));
                                Log.e(debuggingString, String.valueOf(pairs.get(i).getLatitude()));
                                Log.e(debuggingString, String.valueOf(pairs.get(i).getLongitude()));*/
                            }
                            firstpass = false;
                        } else {
                            if (clientCnt == pairs.size()) { //update coordinata elemenata pairs liste ako se nisu spojili novi klijenti
                                for (int i = 0; i < clientCnt; i++) {
                                    name = (String) in.readObject();
                                    latitude = (Double) in.readObject();
                                    longitude = (Double) in.readObject();
                                    pairs.get(i).setName(name);
                                    pairs.get(i).setLatitude(latitude);
                                    pairs.get(i).setLongitude(longitude);
                                    /*Log.e(debuggingString, String.valueOf(pairs.get(i).getName()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLatitude()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLongitude()));*/
                                }
                                updateMarkers();

                            } else { //update coordinate i dodavanje novih elemenata na kraj
                                if(clientCnt > pairs.size()){
                                    int difference = clientCnt - pairs.size();
                                    for (int i = 0; i < pairs.size(); i++) {
                                        name = (String) in.readObject();
                                        latitude = (Double) in.readObject();
                                        longitude = (Double) in.readObject();
                                        pairs.get(i).setName(name);
                                        pairs.get(i).setLatitude(latitude);
                                        pairs.get(i).setLongitude(longitude);
                                    /*Log.e(debuggingString, String.valueOf(pairs.get(i).getName()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLatitude()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLongitude()));*/
                                    }
                                    for (int i = pairs.size(); i < clientCnt; i++) {
                                        final int j = i;
                                        name = (String) in.readObject();
                                        latitude = (Double) in.readObject();
                                        longitude = (Double) in.readObject();
                                        pairs.add(new Pair(name, latitude, longitude));

                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                menu.add(0, j, j, pairs.get(j).getName());

                                                menu.getItem(j).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){

                                                    @Override
                                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                                        int id = menuItem.getItemId();
                                                        showClickedMarkerTitle(id);
                                                        return false;
                                                    }
                                                });

                                            }
                                        });

                                    /*Log.e(debuggingString, String.valueOf(pairs.get(i).getName()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLatitude()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLongitude()));*/
                                    }
                                    updatePositionsArray(difference);
                                    updateMarkersArray(difference);
                                    updateMarkers();
                                }

                                if(clientCnt < pairs.size()){
                                    int difference = clientCnt - pairs.size();
                                    for (int i = 0; i < clientCnt; i++) {
                                        name = (String) in.readObject();
                                        latitude = (Double) in.readObject();
                                        longitude = (Double) in.readObject();
                                        pairs.get(i).setName(name);
                                        pairs.get(i).setLatitude(latitude);
                                        pairs.get(i).setLongitude(longitude);
                                    /*Log.e(debuggingString, String.valueOf(pairs.get(i).getName()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLatitude()));
                                    Log.e(debuggingString, String.valueOf(pairs.get(i).getLongitude()));*/
                                    }
                                    for (int i = clientCnt; i < pairs.size(); i++) {
                                        final int j = i;
                                        pairs.remove(i);

                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                menu.removeItem(j);
                                            }
                                        });
                                    }
                                    updatePositionsArray(difference);
                                    updateMarkersArray(difference);
                                    updateMarkers();
                                }
                            }
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } catch (
                        Exception e
                        ) {
                    Log.e(debuggingString, "No clientCnt");
                    positions.clear();
                    markers.clear();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Log.e(debuggingString, "Clearing...");
                            menu.clear();
                            Log.e(debuggingString, "Menu cleared");
                            updateMarkers();
                            Log.e(debuggingString, "Markers cleared");
                        }
                    });
                }
            }
        }.start();
    }

    private void updatePositionsArray(int difference) {
        if(difference > 0){
            for (int i = (pairs.size() - difference); i < clientCnt; i++) {
                positions.add(new LatLng(pairs.get(i).getLatitude(), pairs.get(i).getLongitude()));
            }
        }else if (difference < 0){
            positions.clear();
            for (int i = (pairs.size() - difference); i < clientCnt; i++) {
                positions.add(new LatLng(pairs.get(i).getLatitude(), pairs.get(i).getLongitude()));
            }
        }

    }

    private void updateBoundaries() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }
                LatLngBounds bounds = builder.build();

                final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 400);

                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mMap.animateCamera(cu);
                    }
                });
            }
        });
    }

    private void updateMarkersArray(final int difference) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(difference > 0){
                    for (int i = (pairs.size() - difference); i < clientCnt; i++) {
                        markers.add(mMap.addMarker(new MarkerOptions()
                                .position(positions.get(i))
                                .title(pairs.get(i).getName())));
                    }
                }else if (difference < 0){
                    markers.clear();
                    for (int i = 0; i < pairs.size(); i++) {
                        markers.add(mMap.addMarker(new MarkerOptions()
                                .position(positions.get(i))
                                .title(pairs.get(i).getName())));
                    }
                }
            }
        });
    }

    private void showClickedMarkerTitle(final int id) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                for(int i = 0; i < markers.size(); i++){
                    if (id == i){
                        Log.e(debuggingString, String.valueOf(id + " Selected"));
                        markers.get(i).showInfoWindow();
                    }
                }

                double lat = pairs.get(id).getLatitude();
                double lng = pairs.get(id).getLongitude();

                final LatLng position = new LatLng(lat,lng);

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                //Log.e(debuggingString, String.valueOf(item.getTitle()) + " Selected");
                //Log.e(debuggingString, String.valueOf(id + " ID Selected"));

                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17));
                    }
                });
            }
        });
    }

    private void updateMarkers() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(markers.size() == 0){
                    mMap.clear();
                    return  ;
                }
                for (int i = 0; i < pairs.size(); i++) {
                    markers.get(i).setPosition(new LatLng(pairs.get(i).getLatitude(), pairs.get(i).getLongitude()));
                }
                updateBoundaries();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();

        return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        for (int i = 0; i < clientCnt; i++) {
            positions.add(new LatLng(pairs.get(i).getLatitude(), pairs.get(i).getLongitude()));
        }

        for (int i = 0; i < clientCnt; i++) {
            markers.add(mMap.addMarker(new MarkerOptions()
                    .position(positions.get(i))
                    .title(pairs.get(i).getName())));
            markers.get(i).setTag(i);
        }
    }
}


