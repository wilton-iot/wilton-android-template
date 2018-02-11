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
], function() {
    "use strict";

    var mainActivity = Packages.net.wiltontoolkit.android.MainActivity.INSTANCE;

    function initWebView() {
        // init webview
        var webView = mainActivity.findViewById(Packages.net.wiltontoolkit.android.R.id.activity_main_webview);
        // Force links and redirects to open in the WebView instead of in a browser
        webView.setWebViewClient(new Packages.android.webkit.WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://127.0.0.1:8080/");
    }

    return {
        main: function() {
            mainActivity.runOnUiThread(new Packages.java.lang.Runnable({
                run: function() {
                    try {
                        // web view
                        initWebView();

                    } catch(e) {
                        mainActivity.showMessage(e.message + "\n" + e.stack);
                    }
                }
            }));
        }
    };

});
