package com.bogomolov.alexander.weatherwidget;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

/**
 * Created by admin on 09.11.2017.
 */

public class UpdateService extends Service {
    public static final String UPDATE_URL = "https://query.yahooapis.com/v1/" +
            "public/yql?q=select%20wind,atmosphere,item%20from%20weather" +
            ".forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo" +
            ".places(1)%20where%20text%3D%22kharkiv%2Cukraine%22)&format=xml";


    @Override
    public void onStart(Intent intent, int startId) {
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(this.getApplicationContext());

        int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager
                .EXTRA_APPWIDGET_IDS);
        for (int widgetId: allWidgetIds) {
            buildUpdate();

            RemoteViews remoteViews = new RemoteViews(
                    this.getApplicationContext().getPackageName(),
                    R.layout.weather_widget);

            Intent clickIntent = new Intent(this.getApplicationContext(),
                    WeatherWidgetProvider.class);

            clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    allWidgetIds);

            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(getApplicationContext(), 0, clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        stopSelf();

        super.onStart(intent, startId);
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void buildUpdate() {
        if (!networkAvailable()) {
            RemoteViews views = new RemoteViews(UpdateService.this.getPackageName(),
                    R.layout.weather_widget);

            views.setTextViewText(R.id.temperature, "Temperature: no connection");
            views.setTextViewText(R.id.pressure, "Pressure: no connection");
            views.setTextViewText(R.id.humidity, "Humidity: no connection");
            views.setTextViewText(R.id.wind, "Wind: no connection");

            ComponentName thisWidget = new ComponentName(UpdateService.this,
                    WeatherWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(
                    UpdateService.this);
            manager.updateAppWidget(thisWidget, views);

            Intent dialogIntent = new Intent(this, ConnectionDialog.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
            return;
        }


        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Weather weather = parseResponse(response);

                RemoteViews views = new RemoteViews(UpdateService.this.getPackageName(),
                        R.layout.weather_widget);

                views.setTextViewText(R.id.temperature,
                        weather.getTemperature() + (char) 0x00B0 + "F");

                String pressure = weather.getPressure();
                if (pressure != null) {
                    views.setTextViewText(R.id.pressure, "Pressure: " + pressure + "in");
                } else {
                    views.setTextViewText(R.id.pressure, "Pressure: no data");
                }

                String humidity = weather.getHumidity();
                if (humidity != null) {
                    views.setTextViewText(R.id.humidity, "Humidity: " + humidity + "%");
                } else {
                    views.setTextViewText(R.id.humidity, "Humidity: no data");
                }

                views.setTextViewText(R.id.wind, "Wind: " + weather.getWind() + " mph");

                ComponentName thisWidget = new ComponentName(UpdateService.this,
                        WeatherWidgetProvider.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(
                        UpdateService.this);
                manager.updateAppWidget(thisWidget, views);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        };

        StringRequest req = new StringRequest(Request.Method.GET, UPDATE_URL,
                listener, errorListener);

        RequestQueue rq = Volley.newRequestQueue(UpdateService.this);
        rq.add(req);
    }

    private Weather parseResponse(String response) {
        Weather weather = new Weather();

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance()
                    .newPullParser();
            parser.setInput(new StringReader(response));

            String tagName = "";

            int event = parser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();

                if (event == XmlPullParser.START_TAG) {
                    switch (tagName) {
                        case "yweather:wind":
                            weather.setWind(parser.getAttributeValue(3));
                            break;
                        case "yweather.atmosphere":
                            weather.setHumidity(parser.getAttributeValue(1));
                            weather.setPressure(parser.getAttributeValue(2));
                            break;
                        case "yweather:condition":
                            weather.setTemperature(parser.getAttributeValue(3));
                            weather.setSky(parser.getAttributeValue(4));
                            break;
                    }
                }
                event = parser.next();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return weather;
    }

    private boolean networkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
