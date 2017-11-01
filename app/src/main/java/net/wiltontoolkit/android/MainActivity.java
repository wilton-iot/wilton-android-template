package net.wiltontoolkit.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.os.*;
import android.util.Log;
import android.webkit.WebView;

import net.wiltontoolkit.support.rhino.WiltonRhinoEnvironment;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MainActivity extends Activity {

    private Context rhinoContext;
    private ScriptableObject rhinoScope;

    // Activity callbacks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Executors.newSingleThreadExecutor(new DeepThreadFactory())
                .execute(new StartAppTask());
    }

    @Override
    public void onBackPressed() {
        WebView wv = findViewById(R.id.activity_main_webview);
        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }


    // Application startup logic, runs on rhino-thread

    private void startApplication() {
        // assets
        unpackAssets();

        // run app
        WiltonRhinoEnvironment.initialize("");
        rhinoContext = Context.enter();
        rhinoScope = rhinoContext.initStandardObjects();
        ScriptableObject.putProperty(rhinoScope, "GLOBAL_ACTIVITY", Context.javaToJS(MainActivity.this, rhinoScope));
        File initJs = new File(getExternalFilesDir(null), "init.js");
        runJsFile(initJs);
        Context.exit();
    }


    // exposed to JS


    public void loadJniClass() {
        try {
            Class.forName("net.wiltontoolkit.WiltonJni");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void runJsFile(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            Reader reader = new InputStreamReader(is, "UTF-8");
            rhinoContext.evaluateReader(rhinoScope, reader, file.getAbsolutePath(), 1, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(is);
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


    // helper methods

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

    private class StartAppTask implements Runnable {
        @Override
        public void run() {
            try {
                startApplication();
            } catch (Throwable e) {
                logError(e);
            }
        }
    }

    private static class DeepThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(new ThreadGroup("rhino"), r, "rhino-thread", 1024 * 1024 * 16);
        }
    }
}
