
define([
    "wilton/Channel",
    "wilton/net",
    "wilton/thread"
], function(Channel, net, thread) {
    "use strict";

    return {
        main: function() {
            new Channel("signal", 1);
            thread.run({
                callbackScript: {
                    module: "bootstrap/index",
                    func: "main"
                }
            });

            net.waitForTcpConnection({
                ipAddress: "127.0.0.1",
                tcpPort: 8080,
                timeoutMillis: 20000
            });
        }
    };
});
