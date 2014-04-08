/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */

var urlParams;
var madeButtons = false;

var currentGuiClips = [];
var currentKeyClips = [];

var currentView = 0;

function highlightNthButton(n) {
    if (currentGuiClips.length + currentKeyClips.length > 1) {
        var buttons = $("#viewOtherDiv").find("button");
        buttons.removeClass("activated");
        buttons.eq(n).addClass("activated");
    }
}

function modifyMultipleClipButtonsForLocal() {
    $("#viewOtherDiv").find("button").data("source", "local");
}

function makeButtonsForMultipleGuiAndKeyClips() {
    var i, j, newButton, insertionPoint;

    if (currentKeyClips.length + currentGuiClips.length > 1) {
        //clear out old buttons
        $("#viewOtherDiv").find("button").remove();
        $("#viewOtherDiv").find("div").remove();
        $("#viewOtherDiv").show();

        insertionPoint = $("#viewOtherSpot");
        for (i = 0; i < currentGuiClips.length; i++) {
            newButton = $('<button class="viewOther"></button>');
            newButton.text("Example " + (i + 1));
            newButton.data("index", i);
            insertionPoint.before(newButton);
        }
        if (currentGuiClips.length === 0) {
            insertionPoint.before("<div class='noClipsShared'>No clips demonstrating GUI use</div>");
        }

        insertionPoint.before("<div>More keyboard shortcut uses:</div>");
        insertionPoint.before("<div></div>");

        for (j = 0; j < currentKeyClips.length; j++) {
            newButton = $('<button class="viewOther"></button>');
            newButton.text("Example " + (j + 1));
            newButton.data("index", i + j);
            insertionPoint.before(newButton);
        }
        if (currentKeyClips.length === 0) {
            insertionPoint.before("<div class='noClipsShared'>No clips demonstrating keyboard shortcut use</div>");
        }

    }
    else {
        $("#viewOtherDiv").hide();
    }
}

//Wraps around from GUIclips to KeyClips
function getIthClip(clipIndex) {
    if (clipIndex < currentGuiClips.length) {
        return currentGuiClips[clipIndex];
    }
    return currentKeyClips[clipIndex - currentGuiClips.length];
}

function changeLocalMediaSource(clipIndex) {
    var postUrl, currentImageDir, clipName;

    clipName = getIthClip(clipIndex);

    currentImageDir = "/" + clipName;
    postUrl = "/mediaServer";

    makeButtonsForMultipleGuiAndKeyClips();
    modifyMultipleClipButtonsForLocal();
    highlightNthButton(clipIndex);

    //clear out all frames
    $(".frame").remove();

    //reload frames //TODO: implement caching
    $.ajax({
        type: "POST",
        url: postUrl,
        data: { "clipName": clipName, "thingToDo": "getImages" },
        success: function (data) {
            data.clip.plugin = urlParams.pluginName;
            data.clip.tool = urlParams.toolName;
            setUpPlaybackForDataAuthAndDir(data, "", currentImageDir);

        },
        error: function (error, e, f) {
            console.log("error");
            console.log(error);
            console.log(e);
            console.log(f);
        }
    });
}

function showLocalClips(guiClips, keyClips) {
    stopFramePlayback();
    $("#placeholder").hide();
    if (guiClips.length + keyClips.length === 0) {
        $("#clipLoading").hide();
        $("#clipDoesNotExist").show();
    }
    else {
        $("#clipDoesNotExist").hide();
        $("#clipLoading").show();

        currentGuiClips = guiClips;
        currentKeyClips = keyClips;

        changeLocalMediaSource(0);		//start with the first clip

    }
}


function checkExistanceOfLocalClips(pluginName, toolName) {
    var postUrl = "/mediaServer";

    $("#clipPlayer").hide();

    $.ajax({
        type: "POST",
        url: postUrl,
        data: { "pluginName": pluginName, "toolName": toolName, "thingToDo": "queryClipExistance" },
        success: function (data) {
            console.log(data);
            console.log(JSON.stringify(data));
            //Expecting a {guiclips: [CLIPID, CLIPID...], keyclips: [CLIPID, CLIPID...]}
            showLocalClips(data.guiclips, data.keyclips);

        },
        error: function () {
            console.log("There was a problem");
        }

    });
}

function viewOtherExample() {
    var e = $(this);

    currentView = e.data("index");
    changeLocalMediaSource(e.data("index"));

    if ($(".viewOther").eq(currentView).hasClass("shared")) {
        $("#sidebar .shareClip").prop("disabled", true);
        $("#sidebar .shareClip").addClass("disabled");
        $("#sidebar .shareClip").text("Shared!");
    }
    else {
        $("#sidebar .shareClip").prop("disabled", false);
        $("#sidebar .shareClip").removeClass("disabled");
        $("#sidebar .shareClip").text("Share This Clip");
    }

}

//pulls the params from the url bar to the global variable urlParams
//http://stackoverflow.com/a/2880929/1447621
(window.onpopstate = function () {
    var match,
        pl = /\+/g,  // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
        query = window.location.search.substring(1);

    urlParams = {};
    while (match = search.exec(query)) {
        urlParams[decode(match[1])] = decode(match[2]);
    }
})();

function shareNoClips() {
    $("#moreInfo").hide();
    $("#sidebar").html("<h2>Thanks for your input</h2");

    //TODO report that no clips where shared?
}

function shareClip() {
    $(".viewOther").eq(currentView).addClass("shared");
    $("#sidebar .shareClip").prop("disabled", true);
    $("#sidebar .shareClip").addClass("disabled");
    $("#sidebar .shareClip").text("Shared!");

    $.ajax({
        type: "post",
        url: "/shareClip",
        data: { clipId: getIthClip(currentView), recipient: urlParams.shareWithEmail }
    });
}

function shareAll() {
    $(".viewOther").eq(currentView).addClass("shared");
    $("#sidebar .shareClip").prop("disabled", true);
    $("#sidebar .shareClip").addClass("disabled");
    $("#sidebar .shareClip").text("Shared!");

    $.ajax({
        type: "post",
        url: "/shareClip",
        data: { clipId: getIthClip(currentView), recipient: "all" }
    });
}

$(document).ready(function () {

    console.log(urlParams);

    var userHeader = $("#userHeader");
    userHeader.text(urlParams.shareWithName + " [" + urlParams.shareWithEmail + "]");

    $("#viewOtherDiv").on('click', 'button', viewOtherExample);
    $("#sidebar").on('click', '.noClips', shareNoClips);
    $("#sidebar").on('click', '.shareClip', shareClip);
	$("#sidebar").on('click', '.shareAll', shareAll);

    checkExistanceOfLocalClips(urlParams.pluginName, urlParams.toolName);

});