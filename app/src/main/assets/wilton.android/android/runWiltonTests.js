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
