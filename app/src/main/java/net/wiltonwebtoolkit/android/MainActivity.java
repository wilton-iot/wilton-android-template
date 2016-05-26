package net.wiltonwebtoolkit.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MainActivity extends Activity {

    private WebView webView;
    private Context rhinoContext;
    private Global rhinoScope;
    private Executor executor = Executors.newSingleThreadExecutor(new DeepThreadFactory());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assets
        unpackAssets();

        // bottom bar
        hideBottomBar();

        // close button
        Button button = (Button) findViewById(R.id.close_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Process.killProcess(Process.myPid());
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                initRhino();
                File initScript = new File(getExternalFilesDir(null), "init.js");
                runScript(initScript);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // init webview
                        webView = (WebView) findViewById(R.id.activity_main_webview);
                        // Force links and redirects to open in the WebView instead of in a browser
                        webView.setWebViewClient(new WebViewClient());
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.loadUrl("http://127.0.0.1:8080/");
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        WebView wv = (WebView) findViewById(R.id.activity_main_webview);
        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void showMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .show();
            }
        });
    }

    public String getWorkDirectory() {
        return getExternalFilesDir(null).getAbsolutePath();
    }

    private void logError(Throwable e) {
        try {
            final String msg = Log.getStackTraceString(e);
            // write to system log
            Log.e(getClass().getPackage().getName(), Log.getStackTraceString(e));
            // show on screen
            showMessage(msg);
        } catch (Exception e1) {
            // give up
        }
    }

    // existing assets won't be overridden, use app uninstall+install for that
    private void unpackAssets() {
        try {
            File dir = getExternalFilesDir(null);
            AssetManager am = getAssets();
            for (String name : am.list("")) {
                if (0 == am.list(name).length) {
                    unpackAssetFile(dir, name);
                } else if (!("images".equals(name) || "sounds".equals(name) || "webkit".equals(name))) {
                    unpackAssetsDir(dir, name);
                }
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private void unpackAssetsDir(File topLevelDir, String relPath) throws IOException {
        File destDir = new File(topLevelDir, relPath);
        if (destDir.exists()) return;
        boolean created = destDir.mkdir();
        if (!created) throw new IOException("Cannot create dir: [" + destDir.getAbsolutePath() + "]");
        AssetManager am = getAssets();
        for (String name : am.list(relPath)) {
            String childPath = relPath + "/" + name;
            if (0 == am.list(childPath).length) {
                unpackAssetFile(topLevelDir, childPath);
            } else {
                unpackAssetsDir(topLevelDir, childPath);
            }
        }
    }

    private void unpackAssetFile(File topLevelDir, String relPath) throws IOException {
        File destFile = new File(topLevelDir, relPath);
        if (destFile.exists()) return;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = getAssets().open(relPath);
            os = new FileOutputStream(destFile);
            copy(is, os);
            os.close();
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    private void initRhino() {
        rhinoContext = Context.enter();
        rhinoContext.setOptimizationLevel(-1);
        rhinoScope = new Global();
        rhinoScope.init(rhinoContext);
        ScriptableObject.putProperty(rhinoScope, "GLOBAL_APP", Context.javaToJS(this, rhinoScope));
    }

    private void runScript(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            Reader re = new InputStreamReader(is, "UTF-8");
            rhinoContext.evaluateReader(rhinoScope, re, file.getAbsolutePath(), -1, null);
        } catch (Throwable e) {
            logError(e);
        } finally {
            closeQuietly(is);
        }
    }

    // https://developer.android.com/samples/ImmersiveMode/src/com.example.android.immersivemode/ImmersiveModeFragment.html

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    private void hideBottomBar() {

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private static void closeQuietly(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static class DeepThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(new ThreadGroup("js"), r, "js-init-thread", 1024 * 1024 * 16);
        }
    }
}
