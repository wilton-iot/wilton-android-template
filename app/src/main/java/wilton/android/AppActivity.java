/*
 * Copyright 2017, alex at staticlibs.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wilton.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.*;
import android.util.Log;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;

import wilton.WiltonJni;
import wilton.WiltonException;
import wilton.support.rhino.WiltonRhinoEnvironment;

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
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class AppActivity extends Activity {

    // launchMode="singleInstance" is used
    public static AppActivity INSTANCE = null;

    // Activity callbacks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (null == INSTANCE) {
            INSTANCE = this;
        }
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        Executors.newSingleThreadExecutor()
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

    // Application startup logic, runs on rhino-thread

    private void startApplication() {
        // assets
        File filesDir = getExternalFilesDir(null);
        unpackAssets(filesDir, getClass().getPackage().getName());

        File libDir = new File(getFilesDir().getParentFile(), "lib");
        // init wilton
        String conf = "{\n" +
        "    \"defaultScriptEngine\": \"duktape\",\n" +
        "    \"applicationDirectory\": \"" + filesDir.getAbsolutePath() + "/\",\n" +
        "    \"wiltonHome\": \"" + filesDir.getAbsolutePath() + "/\",\n" +
        "    \"android\": {\n" + 
        "         \"nativeLibsDir\": \"" + libDir.getAbsolutePath() + "\"\n" +
        "    },\n" +
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
        "            \"vueapp\": \"file://" + filesDir.getAbsolutePath() +"/examples/launcher/work/app\"\n" +
        "        },\n" +
        "        \"packages\": " + loadPackagesList(new File(filesDir, "std.wlib")) +
        "    \n},\n" +
        "    \"compileTimeOS\": \"android\"\n" +
        "}\n";
        WiltonJni.initialize(conf);

        // modules
        dyloadWiltonModule(libDir, "wilton_logging");
        dyloadWiltonModule(libDir, "wilton_loader");
        dyloadWiltonModule(libDir, "wilton_duktape");

        callWiltonFunc("duktape", "android/signalChannel", "main");
        callWiltonFunc("duktape", "vueapp/index", "main");
    }

    // helper methods

    public void showMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(AppActivity.this)
                        .setMessage(message)
                        .show();
            }
        });
    }

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

    private String loadPackagesList(File stdWlib) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(stdWlib);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry en = entries.nextElement();
            if ("wilton-requirejs/wilton-packages.json".equals(en.getName())) {
                InputStream is = null;
                try {
                    is = zipFile.getInputStream(en);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    copy(is, os);
                    return new String(os.toByteArray(), Charset.forName("UTF-8"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    closeQuietly(is);
                }
            }
        }
        throw new WiltonException("Cannot load 'wilton-requirejs/wilton-packages.json' entry,"
                + " ZIP path: [" + stdWlib.getAbsolutePath() + "]");
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
}
