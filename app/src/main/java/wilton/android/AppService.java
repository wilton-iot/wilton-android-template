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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import wilton.WiltonJni;
import wilton.WiltonException;

import wilton.android.R;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class AppService extends Service {

    // launchMode="singleInstance" is used
    public static AppService INSTANCE = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == INSTANCE) {
            INSTANCE = this;
        }

        Notification nf = createNotification();
        startForeground(1, nf);

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

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    // Application startup logic, runs on rhino-thread

    private void startApplication() {
        File filesDir = getExternalFilesDir(null);
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
        "            \"vueapp\": \"file://" + filesDir.getAbsolutePath() +"/app\"\n" +
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

        WiltonJni.wiltoncall("runscript_duktape", "{\"module\": \"wilton/android/initApp\"}");
    }

    // helper methods

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.class.getPackage().getName() + ".notification_icon", true);
        PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setAutoCancel(false)
                .setTicker("Wilton")
                .setContentTitle("Wilton")
                .setContentText("Wilton application is running")
                .setSmallIcon(R.drawable.ic_wilton)
                .setContentIntent(pi)
                .setOngoing(true)
                .setNumber(1)
                .build();
    }

    private void dyloadWiltonModule(File directory, String name) {
        WiltonJni.wiltoncall("dyload_shared_library", "{\n" +
        "    \"name\": \"" + name + "\",\n" +
        "    \"directory\": \"" + directory.getAbsolutePath() + "\"\n" +
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
        } catch (Exception e1) {
            // give up
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
