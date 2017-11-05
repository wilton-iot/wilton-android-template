
define([
    "wilton/Logger"
], function(Logger) {
    "use strict";

    var logger = new Logger("print");

    return {
        main: function() {
            print = function(msg) {
                logger.info(msg);
            };
            /*
            try {
                throw new Error("Stacktrace test, NOT an error");
            } catch(e) {
                logger.error(e);
            }
            */

            require([
                "wilton/test/ChannelTest",
                "wilton/test/CronTaskTest",
                "wilton/test/DBConnectionTest",
                "wilton/test/fsTest",
                "wilton/test/fsPromiseTest",
                "wilton/test/hexTest",
                "wilton/test/httpClientTest",
                "wilton/test/miscTest",
                "wilton/test/mustacheTest",
                "wilton/test/netTest",
                //"wilton/test/PDFDocumentTest",
                // todo: ProcessActivity
                //"wilton/test/processTest",
                "wilton/test/ServerTest",
                "wilton/test/threadTest",
                "wilton/test/utilsTest"
            ], function() {});
        }
    };
});
