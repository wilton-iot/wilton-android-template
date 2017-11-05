package net.wiltontoolkit.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.*;
import android.util.Log;
import android.webkit.WebView;

import net.wiltontoolkit.WiltonJni;
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

    // launchMode="singleInstance" is used
    public static MainActivity INSTANCE = null;

    // Activity callbacks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (null == INSTANCE) {
            INSTANCE = this;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Executors.newSingleThreadExecutor(new DeepThreadFactory())
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startApplication();
                        } catch (Throwable e) {
                            logError(e);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // hideBottomBar();
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        this.setIntent(newIntent);
        /*
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(this.getClass().getPackage().getName() + ".notification_icon")) {
            showMessage("notification icon clicked");
        }
        */
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
        File filesDir = getExternalFilesDir(null);
        unpackAssets(filesDir, getClass().getPackage().getName());

        // init wilton
        //File appdir = new File(filesDir, "app");
        String conf = "{\n" +
        "    \"defaultScriptEngine\": \"duktape\",\n" +
        "    \"applicationDirectory\": \"" + filesDir.getAbsolutePath() + "/\",\n" +
        "    \"environmentVariables\": {\n" + 
        "         \"ANDROID\": true\n" +
        "    },\n" +
        "    \"requireJs\": {\n" +
        "        \"waitSeconds\": 0,\n" +
        "        \"enforceDefine\": true,\n" +
        "        \"nodeIdCompat\": true,\n" +
        "        \"baseUrl\": \"zip://" + filesDir.getAbsolutePath() + "/std.wlib\",\n" +
        "        \"paths\": {\n" +
//        "            \"app\": \"file://" + appdir.getAbsolutePath() +"\",\n" +
        "            \"android\": \"file://" + filesDir.getAbsolutePath() +"/android\",\n" +
        "            \"wilton/test\": \"file://" + filesDir.getAbsolutePath() +"/wilton/test\",\n" +
        "            \"bootstrap\": \"file://" + filesDir.getAbsolutePath() +"/examples/bootstrap\"\n" +
        "        }\n" +
        "    }\n" +
        "}\n";
        WiltonJni.wiltoninit(WiltonRhinoEnvironment.gateway(), conf);

        // modules
        File libDir = new File(getFilesDir().getParentFile(), "lib");
        dyloadWiltonModule(libDir, "wilton_logging");
        dyloadWiltonModule(libDir, "wilton_loader");
        dyloadWiltonModule(libDir, "wilton_duktape");
        dyloadWiltonModule(libDir, "wilton_channel");
        dyloadWiltonModule(libDir, "wilton_cron");
        dyloadWiltonModule(libDir, "wilton_db");
        dyloadWiltonModule(libDir, "wilton_fs");
        dyloadWiltonModule(libDir, "wilton_http");
        dyloadWiltonModule(libDir, "wilton_net");
        dyloadWiltonModule(libDir, "wilton_pdf");
        dyloadWiltonModule(libDir, "wilton_thread");

        // rhino
        String prefix = "zip://" + filesDir.getAbsolutePath() + "/std.wlib/";
        String codeJni = WiltonJni.wiltoncall("load_module_script", prefix + "wilton-requirejs/wilton-jni.js");
        String codeReq = WiltonJni.wiltoncall("load_module_script", prefix + "wilton-requirejs/wilton-require.js");
        WiltonRhinoEnvironment.initialize(codeJni + codeReq);

        /*
        String GIT_URL = "git+ssh://androiddev@192.168.43.165/home/androiddev/android-app";
        String GIT_PASSWORD = "androiddev";
        String GIT_BRANCH = "master";
        callWiltonFuncOnRhino("android/checkout", "main", GIT_URL, GIT_PASSWORD, GIT_BRANCH, appdir.getAbsolutePath());
        */
        callWiltonFunc("rhino", "android/initUI", "main");
        String version = callWiltonFunc("rhino", "android/initUI", "version");
        showMessage("Running tests, version: [" + version + "] ...");
        callWiltonFunc("duktape", "android/runWiltonTests", "main");
        showMessage("Tests finished successfully for engine: [duktape]");
        callWiltonFunc("rhino", "android/runWiltonTests", "main");
        showMessage("Tests finished successfully for engine: [rhino]");
        callWiltonFunc("rhino", "android/runBootstrapExample", "main");
        callWiltonFunc("rhino", "android/initWebView", "main");
    }


    // exposed to JS

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

    private void dyloadWiltonModule(File directory, String name) {
        WiltonJni.wiltoncall("dyload_shared_library", "{\n" +
        "    \"name\": \"" + name + "\",\n" +
        "    \"directory\": \"" + directory.getAbsolutePath() + "\"\n" +
        "}");
    }

    private String callWiltonFunc(String engine, String module, String func, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"");
            sb.append(args[i]);
            sb.append("\"");
        }
        sb.append("]");
        return WiltonJni.wiltoncall("runscript_" + engine, "{\n" +
        "    \"module\": \"" + module + "\",\n" +
        "    \"func\": \"" + func + "\",\n" +
        "    \"args\": " + sb.toString() + "\n" +
        "}");
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

    private void unpackAssets(File topLevelDir, String path) {
        try {
            unpackAssetsDir(topLevelDir, path, path);
        } catch(IOException e) {
            logError(e);
        }
    }

    private void unpackAssetsDir(File topLevelDir, String relPath, String stripPrefix) throws IOException {
        String subRelPath = relPath.substring(stripPrefix.length());
        File destDir = new File(topLevelDir, subRelPath);
        if (!destDir.exists()) {
            boolean created = destDir.mkdir();
            if (!created) {
                throw new IOException("Cannot create dir: [" + destDir.getAbsolutePath() + "]");
            }
        }
        AssetManager am = getAssets();
        for (String name : am.list(relPath)) {
            String childPath = relPath + "/" + name;
            if (0 == am.list(childPath).length) {
                unpackAssetFile(topLevelDir, childPath, stripPrefix);
            } else {
                unpackAssetsDir(topLevelDir, childPath, stripPrefix);
            }
        }
    }

    private void unpackAssetFile(File topLevelDir, String relPath, String stripPrefix) throws IOException {
        String subRelPath = relPath.substring(stripPrefix.length());
        File destFile = new File(topLevelDir, subRelPath);
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

    private static class DeepThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(new ThreadGroup("rhino"), r, "rhino-thread", 1024 * 1024 * 16);
        }
    }
}
