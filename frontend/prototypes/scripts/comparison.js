/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var peoplesNames;
var peoplesNamesIndex;

var userName, userEmail, userToken, currentPlugin;
var authString;
function addResponseHTMLToWindow(data) {
    $(".modal").hide();
    $(".moreInfo").html(data);
    renderPlayback();
}

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


function rotatePeoplesNamesAndTools() {
    var emailToView, getUrl;
    peoplesNamesIndex++;
    peoplesNamesIndex = peoplesNamesIndex % peoplesNames.length;
    $("#otherUsersPlaceHolder").text(peoplesNames[peoplesNamesIndex][0] + "'s Tools");
    emailToView = peoplesNames[peoplesNamesIndex][1];
    $("#otherUsersPlaceHolder").data("email", emailToView);

    //TODO display peoples tools
    getUrl = "http://screencaster-hub.appspot.com/api/" + emailToView + "/" + currentPlugin + authString;

    $.ajax({
        url: getUrl,
        success: function (data) {
            console.log(data);
			console.log(JSON.stringify(data));
        },
        error: function () {
            console.log("There was a problem");
        }

    });
}

function showUniqueTools(event) {
    var parentTable, otherTable;

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
            peoplesNames = data["users"];
			var i;
			for(i = 0;i<peoplesNames.length;i++)
			{
				if (peoplesNames[i][1] == userEmail)
				{
					peoplesNames.splice(i, 1);
					break;
				}
			}
            $("#otherUsersPlaceHolder").text("Click to view Co-workers' Tools");
        }
    });
}

$(document).ready(function () {
    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('mouseenter', handleMouseEnter);

    $(".clickMe").on('mouseleave', handleMouseLeave);

    $("#otherUsersPlaceHolder").on('click', rotatePeoplesNamesAndTools);

    loadPeople();


    $(".showUnique").on("click", showUniqueTools);
    $(".showAll").on("click", showAllTools);

    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    currentPlugin = $("body").data("plugin");
    authString = "?name=" + userName + "&email=" + userEmail + "&token=" + userToken;
});