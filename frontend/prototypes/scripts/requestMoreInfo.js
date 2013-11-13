/*global renderPlayback,stopFramePlayback*/       //depends on playback.js


function addResponseHTMLToWindow(data) {
    $(".modal").hide();
    $(".moreInfo").html(data);
	renderPlayback();
}

function requestGenerationOfMedia() {
	stopFramePlayback();
    $(".moreInfo").removeClass("hidden");
    $(".modal").show();
    $.post("makeVideo", {
        thingToDo: "makeVideo",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //this will return the html to view the media
}

function doesVideoExistHuh() {
	stopFramePlayback();
    $(".moreInfo").removeClass("hidden");
    $.post("makeVideo", {
        thingToDo: "isVideoAlreadyMade",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //This will either return the html to view the media, 
    //or some html prompting the user to make the media
}

$(document).ready(function () {
    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('click', doesVideoExistHuh);

    $(".moreInfo").on('click', '.requestGeneration', requestGenerationOfMedia);
});