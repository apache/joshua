/* Global variables */
var RULES;

/* Load the weight values right off */
$().ready(function() {
    /* Initialize the rules. This will be filled in by sendMeta("list_rules") */
    RULES = new Object();

    $("#ruletable").on('click', '.remove_rule', function() {

        // TODO: remove from RULES
        // key = $(this).closest('tr').text();
        // alert(key);
        // delete RULES[key];

        $(this).closest('tr').remove();
        translate_default();
    });

    /* Initialize the param string */
    // http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
    $.urlParam = function(name, url) {
        if (!url) {
            url = window.location.href;
        }
        var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(url);
        if (!results) { 
            return undefined;
        }
        return results[1] || undefined;
    }

    /* Set the query string, if specified */
    if ($.urlParam('q')) {
        $("#sourceTxt").val($.urlParam('q'));
    }

    /* Set the server, if specified */
    if ($.urlParam('server'))
        $("#server_host").val($.urlParam('server'));

    /* Set the port, if specified */
    if ($.urlParam('port')) {
        $("#server_port").val($.urlParam('port'));

        server_connect($("server_host"), $("server_port"));
    }
});

function server_connect(server, port) {
    sendMeta("get_weights");
    sendMeta("list_rules");

    /* Translate the text */
    if ($("#sourceTxt").val())
        translate();
}

function server_changed(e) {
    if (e.which == 13) {
        server = $("#server_host").val()
        port   = $("#server_port").val()

        if (server && port) {
            server_connect(server, port);
            return false;
        }
    }
}

$('#server_host').keypress(server_changed);
$('#server_port').keypress(server_changed);

// Submit the form when pressing enter in the translation box 
$('#sourceTxt').keypress(function (e) {

    if (e.which == 13 && (e.metaKey || e.ctrlKey)) {
        translate_default();

        return false;
    }
});

/*
 * Retrieves the text from the text box and tokenizes it.
 */
function gettext() {
    var text = $("#sourceTxt").val();
    var tokenizedText = text.replace(/([\.!\?,;"'])/g, " $1 ").replace(/ +/g, " ");
    // alert(text + " -> " + tokenizedText);
    return tokenizedText;
}

/*
 * Returns the name of the server.
 */
function getserver() {
    return $("#server_host").val()
}

/*
 * Returns the name of the port.
 */
function getport() {
    return $("#server_port").val()
}

/* 
 * Makes sure Joshua isn't going to choke on the phrase
 */
function validate_rule(source, target) {
    if (! source || ! source)
        return false;

    source = " " + source + " ";
    target = " " + target + " ";

    var sourceX = source.indexOf(" X ");
    var targetX = target.indexOf(" X "); 
    var sourceY = source.indexOf(" Y ");
    var targetY = target.indexOf(" Y "); 

    // make sure X is found on both sides or neither
    if ((sourceX == -1 && targetX != -1) || (sourceX != -1 && targetX == -1))
        return false;

    // if X is found, check for Y, too
    if (sourceX != -1)
        if ((sourceY == -1 && targetY != -1) || (sourceY != -1 && targetY == -1))
            return false;

    return true;
}

function substitute_nt(phrase) {
    phrase = " " + phrase.trim() + " ";
    phrase = phrase.replace(" X ", " [X,1] ");
    if (phrase.indexOf("\[X,1\]") != -1)
        phrase = phrase.replace(" Y ", " [X,2] ");
    return phrase.trim();
}

/**
 * Adds a rule to the custom grammar housed in the MT system.
 */
$('#add_rule').click(function() {
    var sourcePhrase = $("#addPhrase_source").val().trim();
    var targetPhrase = $("#addPhrase_target").val().trim();

    if (! validate_rule(sourcePhrase, targetPhrase)) {
        alert("Invalid rule!");
        return;
    }

    sourcePhrase = substitute_nt(sourcePhrase);
    targetPhrase = substitute_nt(targetPhrase);

    var ruleStr = "add_rule [X] ||| " + sourcePhrase + " ||| " + targetPhrase;

    // Add word-word alignment if unambiguous
    if (sourcePhrase.split().length == 1 && targetPhrase.split().length == 1)
        ruleStr += " |||   ||| 0-0";

    sendMeta(ruleStr);

    // clear the values
    $("#addPhrase_source").val("");
    $("#addPhrase_target").val("");

    // display the new rule
    display_rule(sourcePhrase, targetPhrase, "");

    // re-translate
    translate_default();

    return false;
});


// $('#add_weight').click(function() {
//     var html = "<div class=\"input-group\">
//                     <span class=\"input-group-addon\" id=\"label-lm\"></span>
//                     <input type="text" class=\"form-control\" value=\"1.0\" size=\"8\" id=\"lm_weight\"
//                            aria-describedby="label-lm" />
//                   </div>";
//     $("#feature_weights").append(html);
// });


// Change the LM weight
$('.weights').keypress(function (e) {
    if (e.which == 13) {
        /* Build a list of all the weights (items in the "weights" class) */
        var message = "set_weights";
        $(".weights").each(function(i, item) {
            message += " " + item.id + " " + item.value;
        });

        sendMeta(message);
        // sendMeta("get_weights");

        translate();

        return false;
    }
});

// Writes the translation results in the first output box
function record_results(data, status) {
    result = "<ul class=\"list-group\">";

    /* This version outputs the 1-best candidate of multiple input sentences */
    $(data.data.translations).each(function(i, item) {
        // result += item.totalScore + " " + item.hyp + "<br/>\n";
        // result += "<li class=\"list-group-item\"><span class=\"badge\">" + (i + 1) + "</span>" + clean(item.translatedText) + "</li>";
        result += "<li class=\"list-group-item\">" + clean(item.translatedText) + "</li>";
    });

    result += "</ul>";
    $("#output").html(result);

    $(".oov").click(function(e) {
        var oov = e.target.innerHTML;
        $("#addPhrase_source").val(oov.toLowerCase());
        $("#addPhrase_target").select();
    });
};

/**
 * Cleans out OOVs
 */
function clean(str) {
    str = str.replace(/(\S+?)_OOV/g, "<span class='oov'>$1</span>");
    str = str.replace(/ ([\.\?,])/g, "$1");
    str = str.replace(/" (.*?) "/g, "\"$1\"");
    return str;
}
    
// Displays an error message in the output box
function failure(msg) {
    $("#output").html("<div class='alert alert-danger'>" + msg + "</div>");
};

/* 
 * Query the Joshua server. We create a JSON object with the query string
 */
function query_server(text, meta, server, port, success_func) {
    // var url = "http://" + $("#server_host").val() + ":" + $("#server_port").val() + "/";
    var url = "http://" + server + ":" + port + "/";
    var request = $.ajax({
        dataType: 'json',
        url: url,
        data: { "q": encodeURIComponent(text),
                "meta": meta },
        success: success_func,
    });

    request.fail(function(jqXHR, textStatus) {
        failure(textStatus);
    });
};

// Writes the translation results in the output box
function meta_received(data, status) {
    var metadata = data.metaData;
    $(data.metadata).each(function(i, item) {
        tokens = item.split(" ");
        type = tokens[0]

        if (type == "weights") {
            var weights = new Object();
            for (i = 1; i < tokens.length; i++) {
                words = tokens[i].split("=");
                weights[words[0]] = words[1];

                div = $("#" + words[0]);
                if (div.length > 0) {
                    div.val(words[1]);
                }
            }
        } else if (type == "custom_rule") {
            fields = item.substring(item.indexOf(" ") + 1).split(" ||| ");
            lhs = fields[0];
            source = fields[1];
            target = fields[2];
            weights = fields[3];
            alignment = fields[4];

            key = source + " ||| " + target;
            if (! RULES[key]) {
                RULES[key] = weights;
                display_rule(source, target, weights);
            }
        }
    });
}

/* Displays a rule */
function display_rule(source, target, weights) {
    html  = "<tr>";
    html += "<td><button type=\"button\" class=\"btn btn-danger btn-xs remove_rule\" value=\"" + source + " ||| " + target + "\" onclick=\"remove_rule(this)\">X</button></td>";
    html += "<td>" + source + "</td>";
    html += "<td>" + target + "</td>";
    html += "</tr>";
    
    $("#ruletable tbody:last-child").prepend(html);
}

/* Deletes a rule from Joshua, removes from display */
function remove_rule(obj) {
    rule = obj.value;
    tokens = rule.split(" ||| ");
    source = tokens[0];
    target = tokens[1];
    undisplay_rule(source, target);
    sendMeta("remove_rule [X] ||| " + source + " ||| " + target);
}

/* Removes a rule from the display */
function undisplay_rule(source, target) {
    $("#ruletable tbody").each(function(i, item) {
        console.log("looking at " + item);
    });
}

/* Sends metadata to the server */
function sendMeta(metadata) {
    server = $("#server_host").val()
    port   = $("#server_port").val()

    query_server("", metadata, server, port, meta_received);
}

/* Obtains all the parameters from text boxes, then calls the appropriate translate function */
function translate_default() {
    text = gettext();
    server = getserver();
    port = getport();
    translate(text, server, port, record_results);
}

/* 
 * Sends the AJAX query to the server. "fun" is the function that gets called when
 * the asynchronous query comes back.
 */
function translate(text, server, port, fun) {
    query_server(text, "", server, port, fun);
}
