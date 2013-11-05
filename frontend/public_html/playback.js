var isPlaying = false;
var currentFrame = 0;
function launchFullScreen(element) {  //From davidwalsh.name/fullscreen
  if(element.requestFullScreen) {
    element.requestFullScreen();
  } else if(element.mozRequestFullScreen) {
    element.mozRequestFullScreen();
  } else if(element.webkitRequestFullScreen) {
    element.webkitRequestFullScreen();
  }
}

function goFullScreenAndStartPlaying()
{
	launchFullScreen($("#panel")[0]);
	
	restartAnimation();
}
function restartAnimation()
{
	$(".animation").hide();
	$(".animation").first().show();
	var totalFrames = totalFrames = +$("#panel").data("totalFrames");
	currentFrame = 0;
	if (!isPlaying)
	{
		isPlaying = true;
		window.setInterval(function(){
			$(".animation").eq(currentFrame).hide();
			currentFrame = (currentFrame + 1) % totalFrames;
			$(".animation").eq(currentFrame).show();
		},200);
	}
}

$(document).ready(function()
{
	$(".animation").first().show();	//so the user sees something
	$("#panel").on("click","img", goFullScreenAndStartPlaying);
	
	$("#playPause").draggable();
	
	preloadImages();
	
	
});

function getImageForFrameNumber(frameNumber)
{
	retVal = '<img class="animation" src="bug8/temp';
	if (frameNumber < 1000)
		retVal += "0";
	if (frameNumber < 100)
		retVal += "0";
	if (frameNumber < 10)
		retVal += "0";
	retVal += frameNumber;
	retVal += '.png"></img>';
	return $(retVal);
}

function preloadImages()
{
	var p = $("#panel");
	var totalFrames = +$("#panel").data("totalFrames");
	for(var i = 1; i< totalFrames;i++)
	{
		getImageForFrameNumber(i).appendTo(p);
	}

}