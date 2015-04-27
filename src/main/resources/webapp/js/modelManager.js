/* 
 * Copyright 2015 Leo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var URL = "../v1/model/resources";
var stringFile="";


//Error constants
var FILEFORMAT = 1, AJAX = 2;
var SEND = 1, DELETE = 2 , OPEN = 3;

//Hide the information divs
cleanBoxes();

document.getElementById('addedFile')
  .addEventListener('change', readSingleFile, false);
  
/*
 * 
 * @param a anonymous function caller for make the lists of rules
 */
$(document).ready(function () {
    getter();
});

/* attaches a submit handler to the form */
$("#modelForm").submit(function (event) {
    var insertionMode = $('input[name="AddingMode"]:checked', '#modelForm').val();
        
    var text;

    text = $("#textual").val();
    
    $("#textual").val("");
    $("#addedFile").val("");
    
    sender(text, insertionMode);
    /* stop form from submitting normally */
    event.preventDefault();
});

/*
 * makes an AJAX call in get mode, for getting the installed monitoring rules.
 */
function getter() {
    var jqXMLHttpGet = $.get(URL, JSONParser, "text")
            .fail(function () {
                detectError(GET);
            });
}


function readSingleFile(e) {
    var file = e.target.files[0];
    if (!file) {
        return;
    }
    var reader = new FileReader();
    reader.onload = function(e) {
        stringFile = e.target.result;
    };
    reader.readAsText(file);
}

/*
 * decides in which of the two possibilities is sended the XML rule
 */
function sender(text, insertionMode) {
    var err = "";
    var dottedLength = 0;
   
    if (text.length != 0) {
        textualSender(text, insertionMode);
    }

    if (stringFile.length != 0) {
        tmp = stringFile;
        textualSender(tmp ,insertionMode);
    }

    if (err.length != 0) {
        return;
    }
}

/*
 * sends the xml wrote in the textarea
 */
function textualSender(text, mode) {
    $.ajax({type: mode,
        url: URL,
        data: text,
        contentType: "application/json",
        cache: false,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status  + " : " + errorThrown);
        },
        success: function (json) {
            showConfirmMessage(SEND);
            tableReloader();
        }
    });
}
function JSONParser(json){
    $('#element').empty();
    $('#element').jsonView(json);
}

function traverse(node, tree) {
    var children = $(tree).children(); 
    node.append(tree.nodeName);
    if (children.length) {
        var ul = $("<ul>").appendTo(node);
        children.each(function () {
            var li = $('<li>').appendTo(ul);
            traverse(li, this);
        });
    } else {
        $('<ul><li> ' + $(tree).text() + '<\/li><\/ul>').appendTo(node);
    }
}

/*
function JSONParser(json){
    var obj = $.parseJSON(json);
    for(var i = 0 ; i < obj.methods.length ; i ++){
        $("#tableOfModels").append("<tr>");
            $("#tableOfModels").append("<td>"+(i+1)+"</td>");
            $("#tableOfModels").append("<td>"+ obj.methods[i].type + "</td>");
            $("#tableOfModels").append("<a href=javascript:redirector('../v1/model/resources/"+obj.methods[i].id+"');>link</a>");
            $("#tableOfModels").append("<td>"+ obj.methods[i].id + "</td>");
            $("#tableOfModels").append("<button onclick=deleteModel('" + obj.methods[i].id + "')><span class='glyphicon glyphicon-remove-circle' aria-hidden='true'></span></button>");
        $("#tableOfModels").append("</tr>");
    }
}
*/
function redirector(dest){
    $.ajax({type: "GET",
        url: dest,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status  + " : " + errorThrown);
            tableReloader();
        },
        success: function (data) {
            showConfirmMessage(OPEN);
            showSpecifiedJSON(data);
            tableReloader();
            
        }
    });
}

function showSpecifiedJSON(data){
    alert("ID: " + data.id);
    alert("Type: " + data.type);
}

function deleteModel(id){
    $.ajax({type: "DELETE",
        url: URL+"/"+id,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status  + " : " + errorThrown);
            tableReloader();
        },
        success: function (xml) {
            showConfirmMessage(DELETE);
            tableReloader();
        }
    });
}

function tableReloader(){
    $("#tableOfModels").empty();
    getter();
}

function detectError(info) {
    // display error
    $("#error").text(info);
    $("success").hide();
    $("#error").show();
}

/*
 * Manages the information div
 */
function showConfirmMessage(type) {
    var res;
    $("#error").hide();
    switch (type) {
        case 1:
            res = "Request performed correctly!!";
            break;
        case 2:
            res = "Model correctly deleted";
            break;
        case 3:
            res = "JSON correctly found";
            break;
    }
    $("#success").text(res);
    $("#success").show();
}

function cleanBoxes(){
    $("#error").hide();
    $("#success").hide();
}

