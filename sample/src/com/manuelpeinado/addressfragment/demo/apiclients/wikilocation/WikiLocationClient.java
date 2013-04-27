package com.manuelpeinado.addressfragment.demo.apiclients.wikilocation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import us.monoid.web.Resty;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.manuelpeinado.addressfragment.Callback;

public class WikiLocationClient {
    private static String BASE_URL = "http://api.wikilocation.org/articles?";
    private static String ENCODING = "UTF-8";
    private static String ARGS = "lat=%s&lng=%s&radius=%s&limit=%s";
    private Task mTask;

    private class Task extends AsyncTask<Void, Void, List<Article>> {
        double lon;
        double lat;
        int limit;
        int radius;
        Callback<List<Article>> callback;

        private Task(double lon, double lat, int limit, int radius) {
            this.lon = lon;
            this.lat = lat;
            this.limit = limit;
            this.radius = radius;
        }

        @Override
        protected List<Article> doInBackground(Void... params) {
            return getArticlesSync(lat, lon, radius, limit);
        }

        @Override
        protected void onPostExecute(List<Article> result) {
            if (callback != null) { 
                callback.onResultReady(result);
            }
        }

        public void setCallback(Callback<List<Article>> callback) {
            this.callback = callback; 
        }
    }

    public List<Article> getArticlesSync(double lat, double lon, int radius, int limit) {
        try {
            String args = String.format(ARGS, encode(lat), encode(lon), encode(radius), encode(limit));
            String json = new Resty().text(BASE_URL + args).toString();
            Gson gson = new Gson();
            WikiLocationResult result = gson.fromJson(json, WikiLocationResult.class);
            return result.articles;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String encode(int limit) throws UnsupportedEncodingException {
        return URLEncoder.encode(Integer.toString(limit), ENCODING);
    }

    private String encode(double lat) throws UnsupportedEncodingException {
        return URLEncoder.encode(Double.toString(lat), ENCODING);
    }

    public void sendRequest(double lat, double lon, int radius, int limit,
            Callback<List<Article>> callback) {
        if (mTask != null) {
            mTask.setCallback(null);
        }
        mTask = new Task(lon, lat, limit, radius);
        mTask.setCallback(callback);
        mTask.execute();
    }

    public void setCallback(Callback<List<Article>> callback) {
        mTask.setCallback(callback);
    }
}