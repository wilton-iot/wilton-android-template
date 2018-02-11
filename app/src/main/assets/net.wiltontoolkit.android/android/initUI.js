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

define([
    "wilton/fs",
    "wilton/Logger",
    "wilton/misc"
], function(fs, Logger, misc) {
    "use strict";

    var mainActivity = Packages.net.wiltontoolkit.android.MainActivity.INSTANCE;
    var appdir = misc.wiltonConfig().applicationDirectory;

    function initCloseButton() {
        var button = mainActivity.findViewById(Packages.net.wiltontoolkit.android.R.id.close_button);
        button.setOnClickListener(new Packages.android.view.View.OnClickListener({
            onClick: function() {
                var nm = mainActivity.getSystemService(Packages.android.content.Context.NOTIFICATION_SERVICE);
                nm.cancelAll();
                Packages.android.os.Process.killProcess(Packages.android.os.Process.myPid());
            }
        }));
    }

    // https://developer.android.com/samples/ImmersiveMode/src/com.example.android.immersivemode/ImmersiveModeFragment.html
    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    function hideBottomBar() {
        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        var uiOptions = mainActivity.getWindow().getDecorView().getSystemUiVisibility();
        var newUiOptions = uiOptions;

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Packages.android.os.Build.VERSION.SDK_INT >= 14) {
            newUiOptions |= Packages.android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Packages.android.os.Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= Packages.android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Packages.android.os.Build.VERSION.SDK_INT >= 18) {
            newUiOptions |= Packages.android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        mainActivity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    function showOngoingNotification() {
        var intent = new Packages.android.content.Intent(mainActivity, mainActivity.getClass());
        intent.setFlags(Packages.android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                Packages.android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(mainActivity.getClass().getPackage().getName() + ".notification_icon", true);
        var pi = Packages.android.app.PendingIntent.getActivity(mainActivity, 1, intent, Packages.android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        var nf = new Packages.android.app.Notification.Builder(mainActivity)
                .setAutoCancel(false)
                .setTicker("Wilton")
                .setContentTitle("Wilton")
                .setContentText("Wilton Control Panel")
                .setSmallIcon(Packages.net.wiltontoolkit.android.R.drawable.ic_wilton)
                .setContentIntent(pi)
                .setOngoing(true)
                .setNumber(1)
                .build();
        var nm = mainActivity.getSystemService(Packages.android.content.Context.NOTIFICATION_SERVICE);
        nm.notify(1, nf);
    }

    return {
        main: function() {
            mainActivity.runOnUiThread(new Packages.java.lang.Runnable({
                run: function() {
                    try {
                        // close button
                        initCloseButton();
                        // bottom bar
                        hideBottomBar();
                        // notification
                        showOngoingNotification();
                    } catch(e) {
                        mainActivity.showMessage(e.message + "\n" + e.stack);
                    }
                }
            }));

            Logger.initialize({
                appenders: [{
                    appenderType: "DAILY_ROLLING_FILE",
                    thresholdLevel: "DEBUG",
                    filePath: appdir + "log.txt"
                }],
                loggers: {
                    staticlib: "INFO",
                    wilton: "INFO"
                }
            });
        },

        version: function() {
            return fs.readFile(appdir + "wilton.version").replace(/\s+/g, "") ;
        }
    };
});
