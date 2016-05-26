/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

define([
    "wilton/wilton"
], function (
        wilton
        ) {
    
    var ExampleModel = function () {
        this.logger = new wilton.Logger("app.models.ExampleModel");
        this.logger.debug("Constructor called");
    };

    ExampleModel.prototype = {
        sayHello: function () {
            this.logger.debug("getAboutText called");
            return "Hello from ExampleModel";
        }
    };
   
    return ExampleModel;
});


