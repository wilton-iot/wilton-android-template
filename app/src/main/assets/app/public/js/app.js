/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

function callBackend() {
    var ta = document.getElementById("myTextArea");
    ta.value = "";
    
    var req = new XMLHttpRequest();
    req.onreadystatechange = function () {
        if (XMLHttpRequest.DONE === req.readyState) {
            ta.value = req.responseText;
        }
    };
    req.open("GET", "/app/example", true);
    req.send();
}
