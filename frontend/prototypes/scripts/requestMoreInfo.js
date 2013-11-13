/*global renderPlayback,stopFramePlayback*/       //depends on playback.js

var peoplesNames;
var peoplesNamesIndex;

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

function handleMouseEnter()
{
	var highlightedToolName = $(this).data("toolName");
	$(".clickMe").each(function(){
		if ($(this).data("toolName") == highlightedToolName)
		{
			$(this).addClass("rowHover");
		}
	});
	
}

function handleMouseLeave()
{
	var highlightedToolName = $(this).data("toolName");
	$(".clickMe").each(function(){
		if ($(this).data("toolName") == highlightedToolName)
		{
			$(this).removeClass("rowHover");
		}
	});
}

function rotatePeoplesNamesAndTools(){
	peoplesNamesIndex++;
	peoplesNamesIndex = peoplesNamesIndex % peoplesNames.length;
	$("#otherPeoplesTools").text(peoplesNames[peoplesNamesIndex] +"'s Tools");
	
	$(".otherPersonsTable").find(".clickMe").show();
	$(".otherPersonsTable").find(".clickMe").each(function(){
		if (Math.random() > .9)
		{
			$(this).hide();
		}
	});
	
}

function showUniqueTools(event)
{
	var parentTable, otherTable;
	
	event.preventDefault();
	parentTable = $(this).closest("table");
	
	if (parentTable.hasClass("myTools"))
	{
		otherTable = $("table.otherPersonsTable");
	}
	else
	{
		otherTable = $("table.myTools");
	}
	$(this).hide();
	parentTable.find(".showAll").show();
	
	parentTable.find(".clickMe").each(function(){
		classToFind = "."+$(this).data("toolName");
		if (otherTable.find(classToFind).length>0)
		{
			$(this).hide();
		}
		else
		{
			$(this).show();
		}
	});
}

function showAllTools(event)
{
	var parentTable;
	
	event.preventDefault();
	parentTable = $(this).closest("table");
	$(this).hide();
	parentTable.find(".showUnique").show();
	//now just show everything
	parentTable.find(".clickMe").show();
}

$(document).ready(function () {
    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('click', doesVideoExistHuh);
	
	$(".clickMe").on('mouseenter', handleMouseEnter);
	
	$(".clickMe").on('mouseleave', handleMouseLeave);

    $(".moreInfo").on('click', '.requestGeneration', requestGenerationOfMedia);
	
	$(".changeName").on('click',rotatePeoplesNamesAndTools);
	
	peoplesNamesIndex = -1;
	peoplesNames = $("#otherPeoplesTools").data("names");
	rotatePeoplesNamesAndTools();
	
	$(".showUnique").on("click", showUniqueTools);
	$(".showAll").on("click", showAllTools);
	
});