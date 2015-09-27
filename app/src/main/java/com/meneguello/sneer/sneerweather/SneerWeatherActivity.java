package com.meneguello.sneer.sneerweather;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by rmp on 27/09/15.
 */
public class SneerWeatherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkLocation();
    }

    private void checkLocation() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS must be enabled to fetch your location. Turn it on in the preferences and try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    checkWeather(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            }, getMainLooper());
        } catch (SecurityException e) {
            //TODO
        }
    }

    private void checkWeather(final Location location) {
        /**ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting()) {
            Toast.makeText(SneerWeatherActivity.this, "The network is unavailable. Connect and try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }*/
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(String.format(
                            "http://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f",
                            location.getLatitude(),
                            location.getLongitude())
                    );

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestProperty("User-Agent", "");
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        Toast.makeText(SneerWeatherActivity.this, "The weather service returns failure. Try again later.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    InputStream is = conn.getInputStream();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    StringBuffer sb = new StringBuffer();

                    String line = "";

                    while ((line=rd.readLine()) != null) {
                        sb.append(line);
                    }

                    conn.disconnect();

                    JSONObject jsonObject = new JSONObject(sb.toString());

                    JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                    String description = weather.getString("description");

                    double temp = jsonObject.getJSONObject("main").getDouble("temp");

                    String nameCity = jsonObject.getString("name");
                    String country = jsonObject.getJSONObject("sys").getString("country");

                    startService(getIntent().<Intent>getParcelableExtra("SEND_MESSAGE").setAction(String.format("%s/%s %dºC/%dºF %s", nameCity, country, (int)(temp - 273.16), (int)((temp - 273.15)* 1.8 + 32), description)));
                    finish();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
