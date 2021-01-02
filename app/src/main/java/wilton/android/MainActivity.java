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
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.webkit.WebView;

import wilton.WiltonJni;
import wilton.support.rhino.WiltonRhinoEnvironment;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static wilton.android.Common.jsonDyload;
import static wilton.android.Common.jsonRunscript;
import static wilton.android.Common.jsonWiltonConfig;
import static wilton.android.Common.unpackAsset;

public class MainActivity extends Activity {

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
                            Log.e(getClass().getPackage().getName(), Log.getStackTraceString(e));
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, AppService.class));
        stopService(new Intent(this, DeviceService.class));
        Process.killProcess(Process.myPid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // hideBottomBar();
    }

    @Override
    public void onBackPressed() {
        /* no-op
        WebView wv = findViewById(R.id.activity_main_webview);
        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
        */
    }


    // Application startup logic, runs on rhino-thread

    private void startApplication() {
        // dirs
        File filesDir = getExternalFilesDir(null);
        File libDir = new File(getFilesDir().getParentFile(), "lib");

        // init
        unpackAsset(this, filesDir, "std.wlib");
        String wconf = jsonWiltonConfig("quickjs", filesDir, libDir, "apps", filesDir.getAbsolutePath() + "/apps");
        Log.i(getClass().getPackage().getName(), wconf);
        WiltonJni.initialize(wconf);

        // modules
        WiltonJni.wiltoncall("dyload_shared_library", jsonDyload(libDir, "wilton_logging"));
        WiltonJni.wiltoncall("dyload_shared_library", jsonDyload(libDir, "wilton_loader"));
        WiltonJni.wiltoncall("dyload_shared_library", jsonDyload(libDir, "wilton_quickjs"));

        // rhino
        String prefix = "zip://" + filesDir.getAbsolutePath() + "/std.wlib/";
        String codeJni = WiltonJni.wiltoncall("load_module_resource", "{ \"url\": \"" + prefix + "wilton-requirejs/wilton-jni.js\" }");
        String codeReq = WiltonJni.wiltoncall("load_module_resource", "{ \"url\": \"" + prefix + "wilton-requirejs/wilton-require.js\" }");
        WiltonRhinoEnvironment.initialize(codeJni + codeReq);
        WiltonJni.registerScriptGateway(WiltonRhinoEnvironment.gateway(), "rhino");

        // startup
        WiltonJni.wiltoncall("runscript_quickjs", jsonRunscript("android-launcher/start", ""));
        WiltonJni.wiltoncall("runscript_rhino", jsonRunscript("wilton/android/initMain", ""));
    }

    private static class DeepThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(new ThreadGroup("rhino"), r, "rhino-thread", 1024 * 1024 * 16);
        }
    }
}
