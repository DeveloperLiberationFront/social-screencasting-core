/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var elementPosition = 0;

function addResponseHTMLToWindow(data) {
    $(".modal").hide();
    $("#moreInfo").html(data);

	renderPlayback();
}

function requestGenerationOfMedia() {
	stopFramePlayback();
    $("#moreInfo").removeClass("hidden");
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
    $("#moreInfo").removeClass("hidden");
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

    $("#moreInfo").on('click', '.requestGeneration', requestGenerationOfMedia);
	
	//figure out where it would be normally
	elementPosition = $('#moreInfo').offset();
	//fix it there for scrolling
	$('#moreInfo').css('position','fixed').css('top',elementPosition.top).css('left',elementPosition.left);
});

/*$(window).scroll(function(){
        if($(window).scrollTop() > elementPosition.top){
            $('#moreInfo').css('position','fixed').css('top',elementPosition.top).css('left',elementPosition.left);
        } else {
            $('#moreInfo').css('position','static');
        }    
});*/