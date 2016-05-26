/** vim: et:ts=4:sw=4:sts=4
 * @license RequireJS 2.2.0 Copyright jQuery Foundation and other contributors.
 * Released under MIT license, http://github.com/requirejs/requirejs/LICENSE
 */
/*global require: false, java: false, load: false */
// https://github.com/requirejs/r.js/blob/27594a409b3d37427ec33bdc151ae8a9f67d6b2b/build/jslib/rhino.js

(function () {
    'use strict';
    require.load = function (context, moduleName, url) {

        load(url);

        //Support anonymous modules.
        context.completeLoad(moduleName);
    };

}());
