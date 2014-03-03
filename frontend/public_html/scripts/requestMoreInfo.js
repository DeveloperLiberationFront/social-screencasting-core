/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var elementPosition = 0;

function addResponseHTMLToWindow(data) {
    $(".modal").hide();
    $("#moreInfo").html(data);

    renderPlayback();
}

/*function requestGenerationOfMedia() {
    stopFramePlayback();
    $("#moreInfo").removeClass("hidden");
    $(".modal").show();
    $.post("mediaServer", {
        thingToDo: "makeVideo",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //this will return the html to view the media
}*/

function checkMediaExistance() {
    stopFramePlayback();
    $("#moreInfo").removeClass("hidden");
    $.post("mediaServer", {
        thingToDo: "isVideoAlreadyMade",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //This will either return the html to view the media, 
    //or some html prompting the user to make the media
}

function changeLocalMediaSource() {
    stopFramePlayback();
    $("#moreInfo").removeClass("hidden");
    $.post("mediaServer", {
        thingToDo: "changeToOtherSource",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName"),
        nthUsage: $(this).data("displayOption")
    }, addResponseHTMLToWindow);
    //this will return the html to view the media
}

$(document).ready(function () {

    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('click', checkMediaExistance);

    $("#moreInfo").on('click', '.viewOther', changeLocalMediaSource);

    //figure out where the more info panel would be normally
    elementPosition = $('#moreInfo').offset();
    //fix it there for scrolling
    $('#moreInfo').css('position', 'fixed').css('top', elementPosition.top).css('left', elementPosition.left);
});
