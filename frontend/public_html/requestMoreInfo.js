$(document).ready(function()
{
//handles the first click to see if a video file exists
	$(".clickMe").on('click',function(){
		$(".moreInfo").removeClass("hidden");
		$.post( "makeVideo", { thingToDo: "isVideoAlreadyMade", pluginName: $(this).data("pluginName"), toolName:$(this).data("toolName")}, handleVideoFileExists);
	});
	
	$(".clickMe").on('mouseenter',function(){
		$(this).addClass("hoverOver");
	});
	$(".clickMe").on('mouseleave',function(){
		$(this).removeClass("hoverOver");
	});

//handles the "second" click to generate said video file
	

});

function handleVideoFileExists( data ) {
	$(".modal").hide();
  $(".moreInfo").html( data );
  addRequestGenerationListeners();
}
function addRequestGenerationListeners(){
	$(".requestGeneration").on('click',function(){
		$(".moreInfo").removeClass("hidden");
		$(".modal").show();
		$.post( "makeVideo", { thingToDo: "makeVideo", pluginName: $(this).data("pluginName"), toolName:$(this).data("toolName")}, handleVideoFileExists);
	});
}