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

//Error constants
var FILEFORMAT = 1, AJAX = 2, GET = 3;
var SEND = 1, DELETE = 2, OPEN = 3;

//Global variable with the referred URL for the rules
var URL = "../v1/monitoring-rules";
var stringFile = "";


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
$("#rulesForm").submit(function (event) {
    var text;

    text = $("#textual").val();

    sender(text);

    /* stop form from submitting normally */
    event.preventDefault();
});

/*
 * makes an AJAX call in get mode, for getting the installed monitoring rules.
 */
function getter() {
    var jqXMLHttpGet = $.get(
            URL, xmlParser, "text"
            )
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
    reader.onload = function (e) {
        stringFile = e.target.result;
    };
    reader.readAsText(file);
}

/*
 * decides in which of the two possibilities is sended the XML rule
 */
function sender(text) {
    var err = "";
    var tmp;

    if (text.length != 0) {
        textualSender(text);
    }

    if (stringFile.length != 0) {
        tmp = stringFile;
        textualSender(tmp);
    }

    if (err.length != 0) {
        return;
    }
}

/*
 * sends the xml wrote in the textarea
 */
function textualSender(text) {
    $.ajax({type: "POST",
        url: URL,
        data: text,
        contentType: "text/xml",
        cache: false,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status + " : " + errorThrown);
            tableReloader();
        },
        success: function (xml) {
            showConfirmMessage(SEND);
            tableReloader();
        }
    });
}

function readSingleFile(e) {
    var file = e.target.files[0];
    if (!file) {
        return;
    }
    var reader = new FileReader();
    reader.onload = function (e) {
        stringFile = e.target.result;
    };
    reader.readAsText(file);
}

function deleteRule(id) {
    $.ajax({type: "DELETE",
        url: URL + "/" + id,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status + " : " + errorThrown);
            tableReloader();
        },
        success: function () {
            showConfirmMessage(DELETE);
            tableReloader();
        }
    });
}

function xmlParser(xml) {
    $('#treeView').show();
    var tree = $.parseXML(xml);
    traverse($('#treeView li'), tree.firstChild);

    $('<b>–<\/b>').prependTo('#treeView li:has(li)').click(function () {
        var sign = $(this).text();
        if (sign == "–")
            $(this).text('+').next().next().children().hide();
        else
            $(this).text('–').next().next().children().show();
    });
}

//Ricerca in profondità nell'albero xml
function traverse(node, tree) {
    var children = $(tree).children();
    var link = URL + "/" + tree.id ;
    if (tree.nodeName.indexOf("monitoringRule") > -1 && tree.nodeName.indexOf("monitoringRules") < 0) {
        node.append(tree.nodeName);
        node.append("<button onclick=deleteRule('" + tree.id + "') class='floatRight'><span class = 'glyphicon glyphicon-trash' aria-hidden='true'/></button>");
    }
    else
        node.append("<span>"+tree.nodeName+"</span>");
    
    if (children.length) {
        var ul = $("<ul>").appendTo(node);
        children.each(function () {
            var li = $('<li>').appendTo(ul);
            traverse(li, this);
        });
    } else {//nodo foglia
        $('<ul><li> ' + $(tree).text() + '<\/li><\/ul>').appendTo(node);
    }
}
/*
 * Parses the XML file of the MonitoringRule-List
 */
/*
 function xmlParser(xml) {
 $(xml).find("monitoringRule").each(
 function () {
 var listOfAttr = ["id", "startEnabled", "timeStep", "timeWindow", "collectedMetric", "monitoredTargets"];
 var subAttr = ["metricName", "monitoredTarget"];
 var fieldValues = new Array(8);
 var separatorAttributesFromFields = 4;
 var listOfTypes = "";
 
 for (var j = 0; j < 6; j++) {
 if (j < separatorAttributesFromFields) {
 fieldValues[j] = $(this).attr(listOfAttr[j]);
 }
 else {
 if (j == 4) {
 fieldValues[j] = $(this).find(listOfAttr[j]).attr(subAttr[0]);
 }
 else {
 $(this).find(listOfAttr[j]).find(subAttr[1]).each(
 function () {
 listOfTypes += $(this).attr("type") + " ";
 }
 );
 fieldValues[j] = listOfTypes;
 }
 }
 }
 
 fieldValues[6] = "<a href=javascript:redirector('" + fieldValues[0] + "');>link</a>";
 fieldValues[7] = "<button onclick=deleteRule('" + fieldValues[0] + "')><span class='glyphicon glyphicon-remove-circle' aria-hidden='true'></span></button>";
 
 $("#tableOfRules").append("<tr>");
 for (var i = 0; i < 8; i++) {
 $("#tableOfRules").append("<td>" + fieldValues[i] + "</td>");
 }
 
 $("#tableOfRules").append("</tr>");
 
 }
 );
 
 }*/

function tableReloader() {
    $('#treeView').empty();
    $('#treeView').append("<li></li>");
    $('#treeView').hide();
    getter();
}

function redirector(id) {
    var dest = URL + "/" + id;
    alert(dest);
    $.ajax({
        type: "GET",
        url: dest,
        error: function (jqXHR, textStatus, errorThrown) {
            detectError(textStatus + " " + jqXHR.status + " : " + errorThrown);
            tableReloader();
        },
        success: function (data) {
            showConfirmMessage(OPEN);
            showXMLRule(data);
            tableReloader();

        }
    });
}

/*
 * Manager of the Warning-div messages according to the error types
 */
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
            res = "Rule correctly deleted";
            break;
        case 3:
            res = "XML correctly found";
            break;
    }
    $("#success").text(res);
    $("#success").show();
}


function cleanBoxes() {
    $("#error").hide();
    $("#success").hide();
}

