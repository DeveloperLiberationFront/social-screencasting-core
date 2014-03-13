/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */

var urlParams;
var madeButtons = false;

var currentClips;

var currentView = 0;

function highlightNthButton(n) {
    if (currentClips.length > 1) {
        var buttons = $("#viewOtherDiv").find("button");
        buttons.removeClass("activated");
        buttons.eq(n).addClass("activated");
    }
}

function modifyMultipleClipButtonsForLocal() {
    $("#viewOtherDiv").find("button").data("source", "local");
}

function makeButtonsOnceForMultipleClips(arrayOfClips) {
    var i, newButton, insertionPoint;
	if (!madeButtons)
	{
		if (arrayOfClips.length > 1) {
			//clear out old buttons
			$("#viewOtherDiv").find("button").remove();
			$("#viewOtherDiv").show();
			insertionPoint = $("#viewOtherSpot");
			for (i = 0; i < arrayOfClips.length; i++) {
				newButton = $('<button class="viewOther"></button>');
				newButton.text("Example " + (i + 1));
				newButton.data("index", i);
				insertionPoint.before(newButton);
			}
		}
		else {
			$("#viewOtherDiv").hide();
		}
	}
	madeButtons = true;
}

function changeLocalMediaSource(arrayOfClips, clipIndex) {
    var postUrl, currentImageDir;
    currentImageDir = "/" + arrayOfClips[clipIndex] + "/";
    postUrl = "/mediaServer";
    currentClips = arrayOfClips;

	makeButtonsOnceForMultipleClips(currentClips);
    modifyMultipleClipButtonsForLocal();
    highlightNthButton(clipIndex);

    //clear out all frames
    $(".frame").remove();

    //reload frames //TODO: implement caching
    $.ajax({
        type: "POST",
        url: postUrl,
        data: { "clipName": arrayOfClips[clipIndex], "thingToDo": "getImages" },
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

function showLocalClips(arrayOfClips) {
    stopFramePlayback();
    $("#placeholder").hide();
    if (arrayOfClips.length === 0) {
        $("#clipLoading").hide();
        $("#clipDoesNotExist").show();
    }
    else {
        $("#clipDoesNotExist").hide();
        $("#clipLoading").show();
        changeLocalMediaSource(arrayOfClips, 0);		//start with the first clip

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
            //Expecting a {clips: [CLIPID, CLIPID...]}
            showLocalClips(data.clips);
			
        },
        error: function () {
            console.log("There was a problem");
        }

    });
}

function viewOtherExample() {
    var e = $(this);

	currentView = e.data("index");
    changeLocalMediaSource(currentClips, e.data("index"));
	
	if ($(".viewOther").eq(currentView).hasClass("shared"))
	{
		$("#sidebar .shareClip").prop("disabled",true);
		$("#sidebar .shareClip").addClass("disabled");
		$("#sidebar .shareClip").text("Shared!");
	}
	else
	{
		$("#sidebar .shareClip").prop("disabled",false);
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

function shareNoClips()
{
	$("#moreInfo").hide();
	$("#sidebar").html("<h2>Thanks for your input</h2");
	
	//TODO report that no clips where shared?
}

function shareClip()
{
	$(".viewOther").eq(currentView).addClass("shared");
	$("#sidebar .shareClip").prop("disabled",true);
	$("#sidebar .shareClip").addClass("disabled");
	$("#sidebar .shareClip").text("Shared!");
	
	$.ajax({
		type:"post",
		url:"/shareClip",
		data:{clipId: currentClips[currentView], recipient: urlParams.shareWithEmail}
		});
}

$(document).ready(function () {

    console.log(urlParams);

    var userHeader = $("#userHeader");
    userHeader.text(urlParams.shareWithName + " [" + urlParams.shareWithEmail + "]");

	$("#viewOtherDiv").on('click', 'button', viewOtherExample);
	$("#sidebar").on('click', '.noClips', shareNoClips);
	$("#sidebar").on('click', '.shareClip', shareClip);
	
    checkExistanceOfLocalClips(urlParams.pluginName, urlParams.toolName);

});