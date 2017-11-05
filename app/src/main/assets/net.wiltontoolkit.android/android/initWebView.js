
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
        run: function() {
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
