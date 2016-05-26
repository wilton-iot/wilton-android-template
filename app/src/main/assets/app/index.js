
// app initialization logic goes in this file
// it will be called from init.js
// the whole JS app can be stored in a separate git repo
// and its git update logic can be moved here from init.js

// setup requirejs
load(GLOBAL_APP.getWorkDirectory() + "/app/lib/require.js");
load(GLOBAL_APP.getWorkDirectory() + "/app/lib/rhino.js");

requirejs.config({
    baseUrl: GLOBAL_APP.getWorkDirectory() + "/app/lib"
});

// start application
load(GLOBAL_APP.getWorkDirectory() + "/app/lib/main.js");
