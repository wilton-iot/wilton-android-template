/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

require([
    "wilton/wilton",
    "app/views/exampleView"
], function (
        wilton,
        exampleView
        ) {

    var appdir = GLOBAL_APP.getWorkDirectory() + "/app/";
    var logger = new wilton.Logger("main");

    var successCb = function (e) {
        logger.debug("Server started");
    };

    var errorCb = function (e) {
        throw e;
    };
    
    new wilton.Server({
        tcpPort: 8080,
        logging: {
            appenders: [{
                appenderType: "DAILY_ROLLING_FILE",
                filePath: appdir + "../logs/app.log",
                thresholdLevel: "DEBUG"
            }]
        },
        documentRoots: [{
                resource: "/public/",
                dirPath: appdir + "public/",
                cacheMaxAgeSeconds: 0
            }],
        callbacks: {
            "/": function (req, resp) {
                logger.debug("root url called");
                resp.sendFile(appdir + "mustache/index.html", {
                    headers: {
                        "Content-Type": "text/html"
                    }
                });
            },
            "/app/example": exampleView
        }
    }, successCb, errorCb);

});
