/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var peoplesNames;
var peoplesNamesIndex;

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
    peoplesNamesIndex++;
    peoplesNamesIndex = peoplesNamesIndex % peoplesNames.length;
    $("#otherPeoplesTools").find(".placeHolder").text(peoplesNames[peoplesNamesIndex] + "'s Tools");

	//TODO display peoples tools

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
            peoplesNames = $("#otherPeoplesTools").data("names");
            rotatePeoplesNamesAndTools();
        }
    });
}

$(document).ready(function () {
    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('mouseenter', handleMouseEnter);

    $(".clickMe").on('mouseleave', handleMouseLeave);

    $(".changeName").on('click', '.placeHolder', rotatePeoplesNamesAndTools);

    loadPeople();


    $(".showUnique").on("click", showUniqueTools);
    $(".showAll").on("click", showAllTools);

});