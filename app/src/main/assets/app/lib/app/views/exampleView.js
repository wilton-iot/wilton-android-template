/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

define([
    "wilton/wilton",
    "app/models/ExampleModel"
], function (
        wilton,
        ExampleModel
        ) {

    var logger = new wilton.Logger("app.views.exampleView");
    var model = new ExampleModel();

    return function(req, resp) {
        logger.debug("view called with req: [" + JSON.stringify(req) + "]");
        resp.send({
            text: model.sayHello()
        });
    };
});
