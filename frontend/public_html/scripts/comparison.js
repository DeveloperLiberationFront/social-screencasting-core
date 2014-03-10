/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */       //depends on playback.js and setUpPlayback.js

var peoplesNames;
var peoplesNamesIndex;

var userName, userEmail, userToken, currentPlugin, currentTool, currentClips, currentImageDir;
var authString;

var sortByCount = true;
var sortHiToLo = false;		//we start sorted like this so next click will sort lo to hi
var sortAToZ = true;

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

function modifyMultipleClipButtonsForLocal() {
    $("#viewOtherDiv").find("button").data("source", "local");
}

function modifyMultipleClipButtonsForExternal() {
    $("#viewOtherDiv").find("button").data("source", "external");
}

//JSON tuple {name="name", count=42}
function sortPluginTuplesCount(a, b) {
    return a.count - b.count;		//sorts so that smaller count numbers come first because we insert smallest elements first
}

//JSON tuple {name="name", count=42}
function sortPluginTupleName(a, b) {
	return b.name.localeCompare(a.name);
}


function drawToolTable(tools) {
    var i, newItem;
	if (sortByCount)
	{
		tools.sort(sortPluginTuplesCount);
    }
	else
	{
		tools.sort(sortPluginTuplesName);
	}

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

function makeButtonsForMultipleClips(arrayOfClips) {
    var i, newButton, insertionPoint;
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

function highlightNthButton(n) {
    if (currentClips.length > 1) {
        var buttons = $("#viewOtherDiv").find("button");
        buttons.removeClass("activated");
        buttons.eq(n).addClass("activated");
    }
}



function changeSharedMediaSource(arrayOfClips, clipIndex) {
    var getUrl, emailToView;

    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentImageDir = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + "/" + arrayOfClips[clipIndex];
    getUrl = currentImageDir + authString;

    currentClips = arrayOfClips;

    makeButtonsForMultipleClips(currentClips);
    modifyMultipleClipButtonsForExternal();
    highlightNthButton(clipIndex);

    //clear out all frames
    $(".frame").remove();

    //reload frames //TODO: implement caching
    $.ajax(getUrl, {
        url: getUrl,
        success: function (data) {
            setUpPlaybackForDataAuthAndDir(data, authString, currentImageDir);

        },
        error: function (error, e, f) {
            console.log("error");
            console.log(error);
            console.log(e);
            console.log(f);
        }
    });
    //this will return the html to view the media
}



function changeLocalMediaSource(arrayOfClips, clipIndex) {
    var postUrl;
    currentImageDir = "/" + arrayOfClips[clipIndex] + "/";
    postUrl = "/mediaServer";
    currentClips = arrayOfClips;

    makeButtonsForMultipleClips(currentClips);
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
            data.clip.plugin = currentPlugin;
            data.clip.tool = currentTool;
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

function viewOtherExample() {
    var e = $(this);

    if (e.data("source") == "external") {
        changeSharedMediaSource(currentClips, e.data("index"));
    }
    else {
        changeLocalMediaSource(currentClips, e.data("index"));
    }

}

function showSharedClips(arrayOfClips) {
    stopFramePlayback();
    $("#placeholder").hide();
    if (arrayOfClips.length === 0) {
        $("#clipLoading").hide();
        $("#clipDoesNotExist").hide();
        $("#requestShare").show();
    }
    else {
        $("#clipDoesNotExist").hide();
        $("#requestShare").hide();
        $("#clipLoading").show();
        changeSharedMediaSource(arrayOfClips, 0);		//start with the first clip

    }
}

function showLocalClips(arrayOfClips) {
    stopFramePlayback();
    $("#placeholder").hide();
    if (arrayOfClips.length === 0) {
        $("#clipLoading").hide();
        $("#requestShare").hide();
        $("#clipDoesNotExist").show();
    }
    else {
        $("#requestShare").hide();
        $("#clipDoesNotExist").hide();
        $("#clipLoading").show();
        changeLocalMediaSource(arrayOfClips, 0);		//start with the first clip

    }
}


function checkExistanceOfShare(element) {
    var target, getUrl, emailToView;
    element.preventDefault();

    target = $(element.currentTarget);
    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentTool = target.data("toolName");

    getUrl = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + authString;

    $("#clipPlayer").hide();

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

function checkExistanceOfLocalClips(element) {
    var target, postUrl;
    element.preventDefault();

    target = $(element.currentTarget);
    currentTool = target.data("toolName");

    postUrl = "/mediaServer";

    $("#clipPlayer").hide();

    $.ajax({
        type: "POST",
        url: postUrl,
        data: { "pluginName": currentPlugin, "toolName": currentTool, "thingToDo": "queryClipExistance" },
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

function sortTableByToolName()
{
	var thisTable = $(this).closest("table");
	
	var elements = thisTable.find("tr").filter(".clickMe");
	
	elements.sortElements(function(a, b) {
		
		var first = $(a.childNodes[0]).text().trim();
		var second = $(b.childNodes[0]).text().trim();
		console.log(first +" < "+second+" ?");
		if (sortAToZ)
		{
			return first.localeCompare(second);
		}
		else
		{
			return second.localeCompare(first);
		}
	});
	sortAToZ = !sortAToZ;
}

function sortTableByCount()
{
	var thisTable = $(this).closest("table");
	
	var elements = thisTable.find("tr").filter(".clickMe");
	
	elements.sortElements(function(a, b) {
		
		var first = +$(a.childNodes[1]).text().trim();
		var second = +$(b.childNodes[1]).text().trim();
		console.log(first +" < "+second+" ?");
		if (sortHiToLo)
		{
			return first - second;
		}
		else
		{
			return second - first;
		}
	});
	sortHiToLo = !sortHiToLo;
}

$(document).ready(function () {
    var elementPosition;
    //handles the click on the view buttons to see if a video file exists
    $("table").on('mouseenter', '.clickMe', handleMouseEnter);
    $("table").on('mouseleave', '.clickMe', handleMouseLeave);

    $("#viewOtherDiv").on('click', 'button', viewOtherExample);

    $("#otherUsersPlaceHolder").on('click', rotatePeoplesNamesAndTools);

    $("table").on('click', ".addedItem", checkExistanceOfShare);
    $("table").on('click', ".myItem", checkExistanceOfLocalClips);
	
	$("table").on('click', ".sortByTool", sortTableByToolName);
	$("table").on('click', ".sortByNum", sortTableByCount);
	
    loadPeople();


    $(".showUnique").on("click", showUniqueTools);
    $(".showAll").on("click", showAllTools);

	//global variables
    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + userName + "&email=" + userEmail + "&token=" + userToken;

    elementPosition = $('#moreInfo').offset();
    //fix it there for scrolling
    $('#moreInfo').css('position', 'fixed').css('top', elementPosition.top).css('left', elementPosition.left);
});