package aimee.protoapps.com.a112823;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.support.v7.app.AppCompatActivity;
import androidx.annotation.Nullable;
import android.widget.Toast;

import java.util.Calendar;
import android.app.AlertDialog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the WebView
        webView = findViewById(R.id.webView);

        // Enable debugging for WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Enable JavaScript and other settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Enable DOM storage

        // Set a WebViewClient to handle loading URLs
        webView.setWebViewClient(new WebViewClient());

        // Enable JavaScript interface to communicate between Java and JavaScript
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");

        // Handle file input for image upload
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                // Create an Intent to allow selection of all image file types
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");  // General wildcard for images

                String[] mimeTypes = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml", "image/tiff"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                startActivityForResult(Intent.createChooser(intent, "Select Image"), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        // Load the HTML file from the assets folder
        webView.loadUrl("file:///android_asset/index.html");
        checkForUpdate();



        // Schedule notifications
        scheduleDailyNotification();
        scheduleMonthlyNotification();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    // Schedule daily notification at 8:30 PM
    private void scheduleDailyNotification() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
        calendar.set(Calendar.MINUTE, 00); // 00 minutes
        calendar.set(Calendar.SECOND, 0); // 0 seconds

        // If the time is in the past, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", "112823");
        intent.putExtra("message", "mahal 8pm na gusto ko lang iremind ka na mahal na mahal kita palagi mwuaaaah!");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent);
        }
    }

    // Schedule monthly notification on the 28th at 6:30 AM
    private void scheduleMonthlyNotification() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 28);
        calendar.set(Calendar.HOUR_OF_DAY, 6); // 6 AM
        calendar.set(Calendar.MINUTE, 30); // 30 minutes
        calendar.set(Calendar.SECOND, 0); // 0 seconds

        // If the date is in the past, schedule for next month
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1);
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", "112823");
        intent.putExtra("message", "mahal happy monthsaryyyy iloveyou always mwuaah!");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 30,
                    pendingIntent);
        }
    }

    // Static inner class for JavaScript to call Java methods
    public static class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        // Example method that can be called from JavaScript
        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }
    private void checkForUpdate() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/ProtonyteTV/Protoview/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String json = response.toString();
                String latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
                String releaseURL = json.split("\"html_url\":\"")[1].split("\"")[0];

                String currentVersion = "9.0"; // Your current version

                if (!latestVersion.equals(currentVersion)) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("112823 Update Available")
                                .setMessage("112823 " + latestVersion + " is available mahal kaya update mo na please! HAHAHA")
                                .setPositiveButton("Update", (dialog, which) -> {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(releaseURL)));
                                })
                                .setNegativeButton("Later", null)
                                .show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
