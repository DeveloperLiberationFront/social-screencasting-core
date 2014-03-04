/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */       //depends on playback.js and setUpPlayback.js

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




function changeSharedMediaSource(arrayOfClips, clipIndex) {
    var getUrl, emailToView;

    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentImageDir = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + "/" + arrayOfClips[clipIndex];
    getUrl = currentImageDir + authString;

    currentClips = arrayOfClips;		//TODO deal with more than one clip

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

function changeLocalMediaSource(arrayOfClips, clipIndex)
{
	var postUrl;
    currentImageDir = "/"+arrayOfClips[clipIndex]+"/";
    postUrl = "/mediaServer";
    currentClips = arrayOfClips;		//TODO deal with more than one clip

	//clear out all frames
	$(".frame").remove();
	
	//reload frames //TODO: implement caching
    $.ajax({
		type:"POST",
        url: postUrl,
		data: {"clipName": arrayOfClips[clipIndex]},
        success: function (data) {
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
	var target, postUrl, emailToView;
    element.preventDefault();

    target = $(element.currentTarget);
    emailToView = peoplesNames[peoplesNamesIndex][1];
    currentTool = target.data("toolName");

   //TODO postUrl = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + "/" + currentTool + authString;
	postUrl = "/mediaServer"
   
	$("#clipPlayer").hide();
	
    $.ajax({
		type:"POST",
        url: postUrl,
		data: {"pluginName": currentPlugin, "toolName": currentTool},
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

$(document).ready(function () {
	var elementPosition;
    //handles the click on the view buttons to see if a video file exists
    $("table").on('mouseenter', '.clickMe', handleMouseEnter);
    $("table").on('mouseleave', '.clickMe', handleMouseLeave);

    $("#otherUsersPlaceHolder").on('click', rotatePeoplesNamesAndTools);

    $("table").on('click', ".addedItem", checkExistanceOfShare);
	$("table").on('click', ".myItem", checkExistanceOfLocalClips);

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