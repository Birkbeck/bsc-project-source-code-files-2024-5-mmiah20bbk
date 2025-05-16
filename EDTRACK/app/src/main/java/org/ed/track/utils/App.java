package org.ed.track.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class App extends Application {

    public static Context context;
    private static final String KEY_LOGGED_IN = "logged_in";
    public static boolean IS_INTRO_DONE = false;
    public static Boolean premium = false;
    protected static Boolean removeAds = false;

    public static final String CHANNEL_ID = "ALARM_SERVICE_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
//        createNotificationChannnel();
    }

    public static Context getContext() {
        return context;
    }

    public static SharedPreferences getPreferenceManager() {
        return getContext().getSharedPreferences("shared_prefs", MODE_PRIVATE);
    }

    public static void saveString(String key, String value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String getString(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getString(key, "");
    }

    public static void saveInt(String key, int value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public static int getInt(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getInt(key, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    public static void saveFloat(String key, float value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public static double getFloat(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getFloat(key, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    public static void saveLogin(boolean value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_LOGGED_IN, value).apply();
    }
    public static void saveBoolean(String key, boolean value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(key, false);
    }

    public static boolean isLoggedIn() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public static void logout() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().clear().apply();
    }

    public static void removeKey(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().remove(key).apply();
    }

    public static void saveStringArrayList(String key, ArrayList<String> list) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();
    }

    public static ArrayList<String> getStringArrayList(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        Gson gson = new Gson();
        String json = sharedPreferences.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }


    private void createNotificationChannnel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void saveFileInDocuments(Context context, URL url, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(String.valueOf(url)));
        request.setTitle(fileName);
        request.setMimeType("application/pdf");
        request.allowScanningByMediaScanner();
        request.setAllowedOverMetered(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Set destination directory
        File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GVMFiles/" + fileName);

        if (!destinationFile.exists()) {
            // File does not exist, enqueue the download
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GVMFiles/" + fileName);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);
        } else {
            // File already exists, show a toast
            Toast.makeText(context, "File already exists", Toast.LENGTH_SHORT).show();
        }

    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        Log.e("isService? ", String.valueOf(App.getContext().getSystemService(Context.ACTIVITY_SERVICE) != null));
        ActivityManager manager = (ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void checkBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);
            Log.d("BatteryOptimization", "Is ignoring battery optimizations: " + isIgnoring);

            if (!isIgnoring) {
                Log.d("BatteryOptimization", "Requesting to ignore battery optimizations");
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                context.startActivity(intent);
            } else {
                Log.d("BatteryOptimization", "App is already ignoring battery optimizations");
            }
        }
    }

    // Check if location (GPS or network) is enabled
//    public static boolean isLocationEnabled(Context context) {
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//    }
//
//    // Show a dialog if GPS is not enabled
//    public static void showGPSNotEnabledDialog(Context context) {
//        new AlertDialog.Builder(context)
//                .setTitle(context.getString(com.google.android.gms.base.R.string.common_google_play_services_enable_title))
//                .setMessage(context.getString(R.string.app_name))
//                .setCancelable(false)
//                .setPositiveButton(context.getString(com.google.android.gms.base.R.string.common_google_play_services_enable_button), (dialog, which) -> {
//                    context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
//                })
//                .show();
//    }

}
