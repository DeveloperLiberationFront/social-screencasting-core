/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var peoplesNames;
var peoplesNamesIndex;

var userName, userEmail, userToken, currentPlugin, currentTool, currentClips, currentImageDir;
var authString;

function handleMouseEnter() {
    var highlightedToolName = $(this).data("toolName");
    $(".clickMe").each(function () {
        if ($(this).data("toolName") == highlightedToolName) {
            $(this).addClass("rowHover");
        }
    });
}

function handleMouseLeave() {
    var highlightedToolName = $(this).data("toolName");
    $(".clickMe").each(function () {
        if ($(this).data("toolName") == highlightedToolName) {
            $(this).removeClass("rowHover");
        }
    });
}

function sortPluginTuples(a, b) {
    return a.count - b.count;		//sorts so that smaller count numbers are better
}



function drawToolTable(tools) {
    var i, newItem;
    tools.sort(sortPluginTuples);
    //console.log(tools);

    //insert them smallest to largest
    for (i = 0; i < tools.length; i++) {
        newItem = $("<tr class='clickMe addedItem' data-tool-name='" + tools[i].name + "'><td>" + tools[i].name + "<td>" + tools[i].count + "</tr>");
        newItem.insertAfter($("#dynamicToolInsertionPoint"));
    }
}


function rotatePeoplesNamesAndTools() {
    var emailToView, getUrl;
    peoplesNamesIndex++;
    peoplesNamesIndex = peoplesNamesIndex % peoplesNames.length;
    $("#otherUsersPlaceHolder").text(peoplesNames[peoplesNamesIndex][0] + "'s Tools");
    emailToView = peoplesNames[peoplesNamesIndex][1];
    $("#otherUsersPlaceHolder").data("email", emailToView);

    $(".addedItem").remove();
    getUrl = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + authString;

    $.ajax({
        url: getUrl,
        success: function (data) {
            var keys, theseTools;
            console.log(data);
            console.log(JSON.stringify(data));
            keys = Object.keys(data[currentPlugin]);
            //turn map data[currentPlugin] to theseTools array
            theseTools = keys.map(function (key) {
                return { name: key, count: data[currentPlugin][key] };
            }
			);
            drawToolTable(theseTools);

        },
        error: function () {
            console.log("There was a problem");
        }

    });

}

function showUniqueTools(event) {
    var parentTable, otherTable;
    //TODO FIX

    event.preventDefault();
    parentTable = $(this).closest("table");

    if (parentTable.hasClass("myTools")) {
        otherTable = $("table.otherPersonsTable");
    }
    else {
        otherTable = $("table.myTools");
    }
    $(this).hide();
    parentTable.find(".showAll").show();

    parentTable.find(".clickMe").each(function () {
        var classToFind = "." + $(this).data("toolName");
        if (otherTable.find(classToFind).length > 0) {
            $(this).hide();
        }
        else {
            $(this).show();
        }
    });
}

function showAllTools(event) {
    var parentTable;
    //TODO FIX

    event.preventDefault();
    parentTable = $(this).closest("table");
    $(this).hide();
    parentTable.find(".showUnique").show();
    //now just show everything
    parentTable.find(".clickMe").show();
}

function loadPeople() {
    $.ajax({
        url: "http://screencaster-hub.appspot.com/api/users",
        success: function (data) {
            peoplesNamesIndex = -1;
            peoplesNames = data.users;

            //remove this user from the array
            for (var i = 0; i < peoplesNames.length; i++) {
                if (peoplesNames[i][1] == userEmail) {
                    peoplesNames.splice(i, 1);
                    break;
                }
            }
            $("#otherUsersPlaceHolder").text("Click to view Co-workers' Tools");
        }
    });
}

function setUpAnimations(imageAssets) {
    //add in animation frames
    var keyInsertionPoint, firstOptionIconInsertionPoint, secondOptionIconInsertionPoint;
    keyInsertionPoint = $("#keyAnimationSpot");
    firstOptionIconInsertionPoint = $("#animationSelectionSpot1");
    secondOptionIconInsertionPoint = $("#animationSelectionSpot2");

    $(".animationSelection").remove();
    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/none.png" />');
    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/none.png" />');

    if (imageAssets.length === 0) {
        return;
    }
	
	firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageAndText.png" />');
    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/textOnly.png" />');
	firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageOnly.png" />');

	secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageAndText.png" />');
    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/textOnly.png" />');
	secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageOnly.png" />');

	keyInsertionPoint.after('<img class="keyAnimation full imageText" src="' + currentImageDir + '/image_text.png'+authString+'" />');
    keyInsertionPoint.after('<img class="keyAnimation blank imageText" src="' + currentImageDir + '/image_text_un.png'+authString+'" />');
	
    keyInsertionPoint.after('<img class="keyAnimation full text" src="' + currentImageDir + '/text.png'+authString+'" />');
    keyInsertionPoint.after('<img class="keyAnimation blank text" src="' + currentImageDir + '/text_un.png'+authString+'" />');
	
	keyInsertionPoint.after('<img class="keyAnimation full image" src="' + currentImageDir + '/image.png'+authString+'" />');
    keyInsertionPoint.after('<img class="keyAnimation blank image" src="' + currentImageDir + '/image_un.png'+authString+'" />');
}

function setUpPlaybackForExternalData(data) {
    var pluginName, toolName, frames, imageAssets, i;
    console.log(data);

    pluginName = data.clip.plugin;
    toolName = data.clip.tool;
    frames = data.clip.filenames;
    for (i = frames.length - 1; i > 0; i--) {
        if (frames[i].lastIndexOf("frame", 0) === 0)	//http://stackoverflow.com/a/4579228/1447621
        {
            break;
        }
    }

    imageAssets = frames.splice(i + 1, frames.length - i - 1);	//animations

    console.log(frames);
    console.log(imageAssets);

    $("#mediaTitle").text(toolName);
    $("#panel").data("totalFrames", frames.length)
				.data("type", (imageAssets.length === 0 ? "menu" : "keystroke"))
				.data("playbackDirectory", currentImageDir)
				.data("auth");

    setUpAnimations(imageAssets);

    renderPlayback(authString);
	$("#externalMediaLoading").hide();
	$("#externalMedia").show();
}


function changeSharedMediaSource(arrayOfClips, clipIndex) {
    var getUrl, emailToView;

    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentImageDir = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + "/" + arrayOfClips[clipIndex];
    getUrl = currentImageDir + authString;

    currentClips = arrayOfClips;

	$(".frame").remove();
	
    $.ajax(getUrl, {
        url: getUrl,
        success: setUpPlaybackForExternalData,
        error: function (error, e, f) {
            console.log("error");
            console.log(error);
            console.log(e);
            console.log(f);
        }
    });
    //this will return the html to view the media
}

function showSharedClips(arrayOfClips) {
    stopFramePlayback();
    $("#placeholder").hide();
    if (arrayOfClips.length === 0) {
        $("#externalMediaLoading").hide();
		
        $("#requestShare").show();
    }
    else {
        $("#requestShare").hide();
        $("#externalMediaLoading").show();
        changeSharedMediaSource(arrayOfClips, 0);		//start with the first clip

    }
}

function checkExistanceOfShare(element) {
    var target, getUrl, emailToView;
    element.preventDefault();

    target = $(element.currentTarget);
    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentTool = target.data("toolName");

    getUrl = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + authString;

	$("#externalMedia").hide();
	
    $.ajax({
        url: getUrl,
        success: function (data) {
            console.log(data);
            console.log(JSON.stringify(data));

            showSharedClips(data.clips);
        },
        error: function () {
            console.log("There was a problem");
        }

    });
}

$(document).ready(function () {
	var elementPosition;
    //handles the click on the view buttons to see if a video file exists
    $("table").on('mouseenter', '.clickMe', handleMouseEnter);
    $("table").on('mouseleave', '.clickMe', handleMouseLeave);

    $("#otherUsersPlaceHolder").on('click', rotatePeoplesNamesAndTools);

    $("table").on('click', ".addedItem", checkExistanceOfShare);

    loadPeople();


    $(".showUnique").on("click", showUniqueTools);
    $(".showAll").on("click", showAllTools);

    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + userName + "&email=" + userEmail + "&token=" + userToken;
	
	elementPosition = $('#moreInfo').offset();
    //fix it there for scrolling
    $('#moreInfo').css('position', 'fixed').css('top', elementPosition.top).css('left', elementPosition.left);
});