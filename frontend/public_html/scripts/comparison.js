/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir */       //depends on playback.js and setUpPlayback.js

var pluginUsers = []; //list of user ids (emails) for users of currentPlugin
var namesByEmail = {}; //map of email -> name
var userData = {}; //cache user tool data

var userName, userEmail, userToken;
var currentPlugin, currentTool, currentImageDir, currentEmail, currentClipIndex;
var currentGuiClips = [];
var currentKeyClips = [];

var authString;

var emailIndex; //index of current email; for rotation purposes

var ascending; //sort order for tables 

var requested = {};

function handleMouseEnter() {
    //$(this).addClass("rowHover");
    var highlightedToolName = $(this).data("toolName"), classToAdd, objs, i;
	classToAdd = "none";
    objs = [];

    $(".clickMe").each(function () {
        if ($(this).data("toolName") &&
            $(this).data("toolName") == highlightedToolName) {
			objs.push($(this));
            if ($(this.childNodes[3]).text().trim().indexOf("/0") != -1) {
                classToAdd = "noKeyboard";
            } else if (classToAdd == "none" ){
                classToAdd = "rowHover";
            }
        }
    });
	
	if (objs.length == 1 && objs[0].hasClass('addedItem')) {
		classToAdd = "noUseBorder";	// don't make things green if they've only used keyboard and they don't have a video
	}

	for(i = 0;i<objs.length;i++)  {
		objs[i].addClass(classToAdd);
	}
}

function handleMouseLeave() {
    $(this).removeClass("rowHover");
    var highlightedToolName = $(this).data("toolName");
    $(".clickMe").each(function () {
        if ($(this).data("toolName") &&
            $(this).data("toolName") == highlightedToolName) {
            $(this).removeClass("rowHover");
			$(this).removeClass("noKeyboard");
			$(this).removeClass("noUseBorder");
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

////JSON tuple {name="name", count=42}
//function sortCount(a, b) {
//    return a.count - b.count;		//sorts so that smaller count numbers come first because we insert smallest elements first
//}

////JSON tuple {name="name", count=42}
//function sortName(a, b) {
//    return b.name.localeCompare(a.name);
//}


function drawToolTable(tools) {
    var i, newItem;

    //insert them smallest to largest
    for (i = 0; i < tools.length; i++) {
        newItem = "<tr class='clickMe addedItem'><td>" + tools[i].name;

        if ("object" == typeof (tools[i].count)) {
            newItem = newItem + "<td>" + tools[i].count.gui + "/" + tools[i].count.keyboard + "</tr>";
        } else {
            newItem = newItem + "<td>" + tools[i].count + "</tr>";
        }

        var hasMatch = false;
        $(".myTools .clickMe").each(function () {
            if ($(this).data("toolName") &&
                $(this).data("toolName") == tools[i].name)
                hasMatch = true;
        });

        newItem = $(newItem);
        if (!hasMatch) newItem.addClass("newTool");
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
    emailIndex = nextIndex(emailIndex);
    currentEmail = pluginUsers[emailIndex];
    showUserTools(currentEmail);
    updateNext();
}

function prevUser() {
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

function selectPlugin() {
    var plugin, url;
    plugin = $(this).val();
    url = window.location.href.replace(
            /[\?#].*|$/, "");
    window.location.href = url + "?pluginName=" + plugin;
    console.log("selected:" + plugin);
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
                ascending = false;		//set ascending to false so that the next call to sort makes them lo to hi
                sortTableByCount($("#otherPersonsTable"));
            },
            error: function () {
                console.log("There was a problem displaying user " + email + "'s tools");
            }
        });
    }
}

function loadPeopleAjax() {
	$("#notTimeYet").hide();
    $.ajax({
        url: "http://screencaster-hub.appspot.com/api/plugin/" + currentPlugin,
        success: function (data) {
            emailIndex = -1;

            //remove this user from the array
            pluginUsers = Object.keys(data.users).filter(function (e) {
                return e != userEmail;
            });

            pluginUsers.forEach(function (email) {
                namesByEmail[email] = data.users[email];
            });

            $("#otherUsersPlaceHolder").text("Click to view Co-workers' Tools");
            listUsers();
        }
    });
}

function loadPeople() {
	$.ajax({
        url: "http://screencaster-hub.appspot.com/api/status",
        success: function (data) {
            if (data.status == "enabled") {
				loadPeopleAjax();
			} else {
				$("#notTimeYet").show();
			}
        }
    });
	
	
}

function listUsers() {
    var email, i, name, newItem;
    for (i = 0; i < pluginUsers.length; i++) {
        email = pluginUsers[i];
        name = namesByEmail[email];

        newItem = $("<tr class='clickMe addedUser'><td>" + name + "<td>" + email + "</tr>");

        newItem.data("index", i);
        newItem.data("email", email);

        newItem.appendTo($("#usersTable tbody"));
    }
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

function highlightNthButtonGuiAndKey(n) {
    if (currentKeyClips.length + currentGuiClips.length > 1) {
        var buttons = $("#viewOtherDiv").find("button");
        buttons.removeClass("activated");
        buttons.eq(n).addClass("activated");
    }
}

//Wraps around from GUIclips to KeyClips
function getIthClip(clipIndex) {
    if (clipIndex < currentGuiClips.length) {
        return currentGuiClips[clipIndex];
    }
    return currentKeyClips[clipIndex - currentGuiClips.length];
}

function changeSharedMediaSource(clipIndex) {
    var getUrl, clipName;
	stopFramePlayback();
    clipName = getIthClip(clipIndex);

    currentImageDir = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + "/" + currentTool + "/" + clipName;
    getUrl = currentImageDir + authString;

    currentClipIndex = clipIndex;

    makeButtonsForMultipleGuiAndKeyClips();
    modifyMultipleClipButtonsForExternal();
    highlightNthButtonGuiAndKey(clipIndex);

    //clear out all frames and animations
    $(".frame").remove();
	$(".keyAnimation").remove();

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




function changeLocalMediaSource(clipIndex) {
    var postUrl, clipName;
	stopFramePlayback();
    clipName = getIthClip(clipIndex);
	
    currentImageDir = "/" + clipName;
    postUrl = "/mediaServer";

    makeButtonsForMultipleGuiAndKeyClips();
    modifyMultipleClipButtonsForLocal();
    highlightNthButtonGuiAndKey(clipIndex);

    //clear out all frames and animations
    $(".frame").remove();
	$(".keyAnimation").remove();

    //reload frames //TODO: implement caching
    $.ajax({
        type: "POST",
        url: postUrl,
        data: { "clipName": clipName, "thingToDo": "getImages" },
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
        changeSharedMediaSource(e.data("index"));
    }
    else {
        changeLocalMediaSource(e.data("index"));
    }

}

function showSharedClips(arrayOfGuiClips, arrayOfKeyClips) {
    var totalClips;
    stopFramePlayback();
    $("#placeholder").hide();
    $("#rating").show();
    
    totalClips = arrayOfGuiClips.length + arrayOfKeyClips.length;
	
    if (totalClips === 0) {
        $("#clipLoading").hide();
        $("#clipDoesNotExist").hide();
        $("#requestShare").show();
    }
    else {
        $("#clipDoesNotExist").hide();
        $("#requestShare").hide();
        $("#clipLoading").show();
        currentGuiClips = arrayOfGuiClips;
        currentKeyClips = arrayOfKeyClips;
        changeSharedMediaSource(0);		//start with the first clip	from SHARED
    }
    updateShareRequestButton(); 
}

function showLocalClips(arrayOfGuiClips, arrayOfKeyClips) {
    var totalClips;
    stopFramePlayback();
    $("#placeholder").hide();
    $("#rating").hide();
	
    totalClips = arrayOfGuiClips.length + arrayOfKeyClips.length;
	
    if (totalClips === 0) {
        $("#clipDoesNotExist").show();
        $("#requestShare").hide();
        $("#clipLoading").hide();
    }
    else {
        $("#requestShare").hide();
        $("#clipDoesNotExist").hide();
        $("#clipLoading").show();
        currentGuiClips = arrayOfGuiClips;
        currentKeyClips = arrayOfKeyClips;
        changeLocalMediaSource(0);		//start with the first clip in LOCAL

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

            showSharedClips(data.guiclips, data.keyclips);
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
        data: { "pluginName": currentPlugin, "toolName": currentTool, "thingToDo": "queryClipExistance"},
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

function sortTableByToolName(givenTable) {
    var elements, thisTable;
    thisTable = (givenTable.hasOwnProperty('target') ? $(givenTable.target).closest("table") : givenTable);

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

function sortTableByCount(givenTable) {
    var elements, thisTable;
    thisTable = (givenTable.hasOwnProperty('target') ? $(givenTable.target).closest("table") : givenTable);

    elements = thisTable.find("tr").filter(".clickMe");

    elements.sortElements(function (a, b) {
        var first, second;
        first = $(a.childNodes[1]).text().trim();
		if (first.indexOf("/") == -1) {
			first = +first;
		} else {
			first = first.split("/");
			first = +first[0] + +first[1];		//converts the 5/8 to (int)5 + (int)8
		}
        second = $(b.childNodes[1]).text().trim();
		if (second.indexOf("/") == -1) {
			second = +second;
		} else {
			second = second.split("/");
			second = +second[0] + +second[1];		//converts the 5/8 to (int)5 + (int)8
		}
		
        if (ascending) {
            return first - second;
        } else {
            return second - first;
        }
    });
    ascending = !ascending;
}

function sortUsersByEmail() { }
function sortUsersByName() { }

function submit_rating() {
    var url = "http://screencaster-hub.appspot.com/api/" +
        currentEmail + "/" +
        currentPlugin + "/" +
        currentTool + "/" +
        getIthClip(currentClipIndex) + authString;
    $.post(url, $(this).serialize());
}

$(document).ready(function () {
    var elementPosition;
    //global variables
    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + userName + "&email=" + userEmail.replace(/\+/g, "%2b") + "&token=" + userToken;

    elementPosition = $('#moreInfo').offset();
    //fix it there for scrolling
    $('#moreInfo').css('position', 'fixed').css('top', elementPosition.top).css('left', elementPosition.left);

    $("#pluginSelector").change(selectPlugin);

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

    $("form#rating").click(submit_rating);

    loadPeople();

    console.log("end of comparison");
});
