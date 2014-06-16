/*global stopFramePlayback, setUpPlaybackForDataAuthAndDir, escape */       //depends on playback.js and setUpPlayback.js

var pluginUsers = []; //list of user ids (emails) for users of currentPlugin
var namesByEmail = {}; //map of email -> name
var userData = {}; //cache user tool data

var userName, userEmail, userToken;
var currentPlugin, currentTool, currentImageDir, currentEmail, currentClipIndex;
var currentGuiClips = [];
var currentKeyClips = [];

var authString;

var emailIndex; //index of current email; for rotation purposes

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
            } else if (classToAdd == "none") {
                classToAdd = "rowHover";
            }
        }
    });

    if (objs.length == 1 && objs[0].hasClass('addedItem')) {
        classToAdd = "noUseBorder";	// don't make things green if they've only used keyboard and they don't have a video
    }

    for (i = 0; i < objs.length; i++) {
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
    var postURL = "/shareRequest";

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
    $("#otherPersonsTable tbody").empty();
    var i, newItem, hasMatch;

    //insert them smallest to largest
    for (i = 0; i < tools.length; i++) {
        newItem = "<tr class='clickMe addedItem'><td>" + tools[i].name;

        if ("object" == typeof (tools[i].details)) {
            newItem = newItem + "<td>" + tools[i].details.gui + "</td><td>" + tools[i].details.keyboard + "</td>";
            if (tools[i].details.hasOwnProperty("clips") &&
                tools[i].details.clips > 0) {
                newItem = newItem + "<td><img src='images/video_icon_tiny.png'/></td>";
            } else {
                newItem = newItem + "<td></td>";
            }
        } else {
            newItem = newItem + "<td>" + tools[i].details + "</td><td></td>";
        }
        newItem = newItem + "</tr>";

        hasMatch = false;
        $(".myTools .clickMe").each(function () {
            if ($(this).data("toolName") && $(this).data("toolName") == tools[i].name) {
                hasMatch = true;
            }
        });

        newItem = $(newItem);
        if (!hasMatch) {
            newItem.addClass("newTool");
        }
        newItem.data("toolName", tools[i].name);
        newItem.appendTo($("#otherPersonsTable tbody"));
       $("table").trigger("update");
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
    var getUrl = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + authString;

    $("#otherUsersPlaceHolder").text(namesByEmail[currentEmail] + "'s Tools");
    $("#usersTable").parent().hide();
    $("#otherPersonsTable").parent().show();
    
    
    $(".addedItem").remove();
    

    if (email in userData) {
        drawToolTable(userData[email]);

        // Click on the "Tool Name" field to sort it.
        $("#otherPersonsTable").children()[0].children[1].children[0].click();
    } else {
        $.ajax({
            url: getUrl,
            success: function (data) {
                var keys, theseTools;
                keys = Object.keys(data[currentPlugin]);
                //turn map data[currentPlugin] to theseTools array
                theseTools = keys.map(function (key) {
                    return { name: key, details: data[currentPlugin][key] };
                });

                userData[email] = theseTools;
                drawToolTable(userData[email]);

                // Click on the "Tool Name" field to sort it.
                $("#otherPersonsTable").children()[0].children[1].children[0].click();
            },
            error: function () {
                console.log("There was a problem displaying user " + email + "'s tools");
            }
        });
    }
}

function hideToolsShowOtherUsers() {
    $("#usersTable").parent().show();
    $("#otherPersonsTable").parent().hide();
}

function loadPeopleAjax() {
    $("#notTimeYet").hide();
    $.ajax({
        url: "http://screencaster-hub.appspot.com/api/details/" + currentPlugin,
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

        newItem = $("<tr class='clickMe addedUser'><td title='"+email+"'>" + name + "</td></tr>");

        newItem.data("index", i);
        newItem.data("email", email);

        newItem.appendTo($("#usersTable tbody"));
        $("table").trigger("update");
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

    currentImageDir = "http://screencaster-hub.appspot.com/api/" + currentEmail + "/" + currentPlugin + "/" + escape(currentTool) + "/" + clipName;
    getUrl = currentImageDir + authString;

    currentClipIndex = clipIndex;

    makeButtonsForMultipleGuiAndKeyClips();
    modifyMultipleClipButtonsForExternal();
    highlightNthButtonGuiAndKey(clipIndex);

    //clear out all frames and animations
    $(".frame").remove();
    $(".keyAnimation").remove();
    
    $("#editDiv").hide();

    //reload frames //TODO: implement caching
    $.ajax({
        type: "GET",
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

    $("#editDiv").show();

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
        data: { "pluginName": currentPlugin, "toolName": currentTool, "thingToDo": "queryClipExistance" },
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

function submit_rating() {
    var url = "http://screencaster-hub.appspot.com/api/" +
        currentEmail + "/" +
        currentPlugin + "/" +
        currentTool + "/" +
        getIthClip(currentClipIndex) + authString;
    $.post(url, $(this).serialize());
}

function setupTableSorting() {
    $("table").not("#otherPersonsTable, #keyTable").tablesorter();

    $.tablesorter.addParser({
        id: "video",
        is: function(s) {
            return false;
        },
        format: function(s, table, cell) {
            return $(cell).children().length;
        },
        type: "numeric"
    });

    $("#otherPersonsTable").tablesorter({
        headers: {
            3: {
                sorter: "video"
            }
        }
    });
}

function setupKey() {
    var keyDiv = $("#keyTableDiv").dialog({
        autoOpen: false
    });

    $("#keyButton").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        keyDiv.dialog("open");
    });
}

$(document).ready(function () {
    var elementPosition;
    //global variables
    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + escape(userName) + "&email=" + escape(userEmail) + "&token=" + escape(userToken);

    elementPosition = $('#moreInfo').offset();

    $("#pluginSelector").change(selectPlugin);

    //handles the click on the view buttons to see if a video file exists
    $("table").on('mouseenter', '.clickMe', handleMouseEnter);
    $("table").on('mouseleave', '.clickMe', handleMouseLeave);

    $("#viewOtherDiv").on('click', 'button', viewOtherExample);

    $("#prevTable").on('click', hideToolsShowOtherUsers);
    $("#nextUser").on('click', nextUser);
    $("#prevUser").on('click', prevUser);

    //users table
    $("table").on('click', ".addedUser", selectUser);

    //items table
    $("table").on('click', ".addedItem", checkExistanceOfShare);
    $("table").on('click', ".myItem", checkExistanceOfLocalClips);

    $(".requestPermissions").on('click', requestSharingPermission);

    $("form#rating").click(submit_rating);

    setupTableSorting();
    setupKey();
    loadPeople();

    console.log("end of comparison");
});
