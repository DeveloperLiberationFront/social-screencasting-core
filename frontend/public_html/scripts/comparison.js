/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */       //depends on playback.js and setUpPlayback.js

var pluginUsers = []; //list of user ids (emails) for users of currentPlugin
var namesByEmail = {}; //map of email -> name
var userData = {}; //cache user tool data

var userName, userEmail, userToken;
var currentPlugin, currentTool, currentClips, currentImageDir, currentEmail;
var authString;

var emailIndex; //index of current email; for rotation purposes

var ascending; //sort order for tables

var requested = {};

function handleMouseEnter() {
    $(this).addClass("rowHover");
    var highlightedToolName = $(this).data("toolName");
    $(".clickMe").each(function () {
        if ($(this).data("toolName") &&
            $(this).data("toolName") == highlightedToolName) {
            $(this).addClass("rowHover");
        }
    });
}

function handleMouseLeave() {
    $(this).removeClass("rowHover");
    var highlightedToolName = $(this).data("toolName");
    $(".clickMe").each(function () {
        if ($(this).data("toolName") &&
            $(this).data("toolName") == highlightedToolName) {
            $(this).removeClass("rowHover");
        }
    });
}

function updateShareRequestButton() {
	$("#requestText").text("You do not have permission to view this user's usages of the tool " + currentTool);

    if (requested[currentEmail + currentPlugin + currentTool] === true) {
        $(".requestPermissions").addClass("requested");
        $(".requestPermissions").prop("disabled", true);
		$(".requestPermissions").text("Requested!");
    }
    else {
        $(".requestPermissions").removeClass("requested");
        $(".requestPermissions").prop("disabled", false);
		$(".requestPermissions").text("Click to Request Permission");
    }
}

function requestSharingPermission() {
    var postURL, emailToRequest;

    postURL = "/shareRequest";

    $.ajax({
        type: "POST",
        url: postURL,
        data: { "pluginName": currentPlugin, "toolName": currentTool, "ownerEmail": currentEmail }

    });

    requested[currentEmail + currentPlugin + currentTool] = true;

    updateShareRequestButton();
}


function modifyMultipleClipButtonsForLocal() {
    $("#viewOtherDiv").find("button").data("source", "local");
}

function modifyMultipleClipButtonsForExternal() {
    $("#viewOtherDiv").find("button").data("source", "external");
}

//JSON tuple {name="name", count=42}
function sortCount(a, b) {
    return a.count - b.count;		//sorts so that smaller count numbers come first because we insert smallest elements first
}

//JSON tuple {name="name", count=42}
function sortName(a, b) {
    return b.name.localeCompare(a.name);
}


function drawToolTable(tools, comparison) {
    var i, newItem;
    comparison = typeof comparison !== 'undefined' ? comparison : sortCount

    //insert them smallest to largest
    for (i = 0; i < tools.length; i++) {
        newItem = $("<tr class='clickMe addedItem'><td>" + tools[i].name + "<td>" + tools[i].count + "</tr>");
		newItem.data("toolName", tools[i].name);
        newItem.appendTo($("#otherPersonsTable tbody"));
    }
}

function nextIndex(i) {
    i++;
    return i % pluginUsers.length;
}

function prevIndex(i) {
    i--;
    i %= pluginUsers.length;
    return i < 0 ? i + pluginUsers.length : i;
}

function updateNext() {
    var next = namesByEmail[pluginUsers[nextIndex(emailIndex)]];
    $("#nextUser").attr("title", "Next: " + next);
}

function updatePrev() {
    var previous = namesByEmail[pluginUsers[prevIndex(emailIndex)]];
    $("#prevUser").attr("title", "Previous: " + previous);
}

function nextUser() {
    var getUrl;
    emailIndex = nextIndex(emailIndex);
    currentEmail = pluginUsers[emailIndex];
    showUserTools(currentEmail);
    updateNext();
}

function prevUser() {
    var getUrl;
    emailIndex = prevIndex(emailIndex);
    currentEmail = pluginUsers[emailIndex];
    showUserTools(currentEmail);
    updatePrev();
}

function selectUser() {
    emailIndex = $(this).data('index');
    currentEmail = $(this).data('email');
    showUserTools(currentEmail);
    updateNext();
    updatePrev();
}

function showUserTools(email) {
    var index, getUrl;
    $("#otherUsersPlaceHolder").text(namesByEmail[currentEmail] + "'s Tools");
    $("#usersTable").hide();
    $("#otherPersonsTable").show();

    $(".addedItem").remove();
    getUrl = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + authString;

    if (email in userData) {
        drawToolTable(userData[email]);
    } else {
        $.ajax({
            url: getUrl,
            success: function (data) {
                var keys, theseTools;
                keys = Object.keys(data[currentPlugin]);
                //turn map data[currentPlugin] to theseTools array
                theseTools = keys.map(function (key) {
                    return { name: key, count: data[currentPlugin][key] };
                });
                userData[email] = theseTools;
                drawToolTable(theseTools);

            },
            error: function () {
                console.log("There was a problem displaying user " + email + "'s tools");
            }
        });
    }
}

function loadPeople() {
    $.ajax({
        url: "http://screencaster-hub.appspot.com/api/plugin/"+currentPlugin,
        success: function (data) {
            emailIndex = -1;

            //remove this user from the array
            pluginUsers = Object.keys(data.users).filter(function(e) {
                return e != userEmail;
            });

            pluginUsers.forEach(function(email) {
                namesByEmail[email] = data.users[email];
            });

            $("#otherUsersPlaceHolder").text("Click to view Co-workers' Tools");
            listUsers();
        }
    });
}

function listUsers() {
    for (var i = 0; i < pluginUsers.length; i++) {
        email = pluginUsers[i];
        name = namesByEmail[email];

        newItem = $("<tr class='clickMe addedUser'><td>" + name + "<td>" + email + "</tr>");
		
		newItem.data("index", i);
		newItem.data("email", email);
		
        newItem.appendTo($("#usersTable tbody"));
    }
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
    var getUrl;
    currentImageDir = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + "/" + currentTool + "/" + arrayOfClips[clipIndex];
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
    updateShareRequestButton();
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
    var target, getUrl;
    element.preventDefault();

    target = $(element.currentTarget);
    currentTool = target.data("toolName");

    getUrl = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + "/" + currentTool + authString;

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

function sortTableByToolName() {
    var elements, thisTable;
    thisTable = $(this).closest("table");

    elements = thisTable.find("tr").filter(".clickMe");

    elements.sortElements(function (a, b) {
        var first, second;
        first = $(a.childNodes[0]).text().trim();
        second = $(b.childNodes[0]).text().trim();
        if (ascending) {
            return first.localeCompare(second);
        } else {
            return second.localeCompare(first);
        }
    });
    ascending = !ascending;
}

function sortTableByCount() {
    var elements, thisTable;
    thisTable = $(this).closest("table");

    elements = thisTable.find("tr").filter(".clickMe");

    elements.sortElements(function (a, b) {
        var first, second;
        first = +$(a.childNodes[1]).text().trim();
        second = +$(b.childNodes[1]).text().trim();
        if (ascending) {
            return first - second;
        } else {
            return second - first;
        }
    });
    ascending = !ascending;
}

function sortUsersByEmail() {}
function sortUsersByName() {}

$(document).ready(function () {
    var elementPosition;
    //global variables
    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + userName + "&email=" + userEmail + "&token=" + userToken;

    elementPosition = $('#moreInfo').offset();
    //fix it there for scrolling
    $('#moreInfo').css('position', 'fixed').css('top', elementPosition.top).css('left', elementPosition.left);

    //handles the click on the view buttons to see if a video file exists
    $("table").on('mouseenter', '.clickMe', handleMouseEnter);
    $("table").on('mouseleave', '.clickMe', handleMouseLeave);

    $("#viewOtherDiv").on('click', 'button', viewOtherExample);

    $("#nextUser").on('click', nextUser);
    $("#prevUser").on('click', prevUser);

    //users table
    $("table").on('click', ".addedUser", selectUser);
    $("table").on('click', ".sortByName", sortUsersByName);
    $("table").on('click', ".sortByEmail", sortUsersByEmail);

    //items table
    $("table").on('click', ".addedItem", checkExistanceOfShare);
    $("table").on('click', ".myItem", checkExistanceOfLocalClips);

    $("table").on('click', ".sortByTool", sortTableByToolName);
    $("table").on('click', ".sortByNum", sortTableByCount);

	$(".requestPermissions").on('click', requestSharingPermission);

    loadPeople();
});
