package aimee.protoapps.com.a112823;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.support.v7.app.AppCompatActivity;
import androidx.annotation.Nullable;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private static final int REQUEST_SAVE_FILE = 100;
    private static final int REQUEST_LOAD_FILE = 101;

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
            // For Android 5.0 and above
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
    }

    // Handle the result of the file chooser and file import/export
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

        // Handle saving the backup file
        if (requestCode == REQUEST_SAVE_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    // Save the DOM storage data to the selected file
                    webView.evaluateJavascript("getDomStorageData();", value -> {
                        saveFile(uri, value);
                    });
                }
            }
        }

        // Handle loading the backup file
        if (requestCode == REQUEST_LOAD_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    String domData = readFile(uri);
                    if (domData != null) {
                        webView.loadUrl("javascript:restoreDomStorageData(" + domData + ");");
                    }
                }
            }
        }
    }

    // Interface for JavaScript to call Java methods
    public class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        /** Call this method from JavaScript to trigger DOM storage backup */
        @JavascriptInterface
        public void exportDomStorage() {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "domstorage_backup.json");
            startActivityForResult(intent, REQUEST_SAVE_FILE);
        }

        /** Call this method from JavaScript to trigger DOM storage restore */
        @JavascriptInterface
        public void importDomStorage() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/json");
            startActivityForResult(intent, REQUEST_LOAD_FILE);
        }
    }

    // Save the DOM storage data to a file
    private void saveFile(Uri uri, String data) {
        try {
            // Use OutputStream instead of FileOutputStream
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os != null) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read the DOM storage data from a file
    private String readFile(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Use InputStream and InputStreamReader instead of FileReader
            InputStream is = getContentResolver().openInputStream(uri);
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
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
