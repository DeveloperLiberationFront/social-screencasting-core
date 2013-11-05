var isPlaying = false;

function launchFullScreen(element) {  //From davidwalsh.name/fullscreen
  if(element.requestFullScreen) {
    element.requestFullScreen();
  } else if(element.mozRequestFullScreen) {
    element.mozRequestFullScreen();
  } else if(element.webkitRequestFullScreen) {
    element.webkitRequestFullScreen();
  }
}

/*function goFullScreenAndStartPlaying()
{
	$(".animation").first().addClass("visible");
	launchFullScreen($("#panel")[0]);
	
	var totalFrames = $(".animation").length;
	var currentFrame = 0;
	window.setInterval(function(){
		$(".animation").eq(currentFrame).removeClass("visible");
		currentFrame = (currentFrame + 1) % totalFrames;
		$(".animation").eq(currentFrame).addClass("visible");
		document.webkitFullscreenElement = $(".animation").eq(currentFrame)[0];
	},500);
}*/

function goFullScreenAndStartPlaying()
{
	launchFullScreen($("#panel")[0]);
	
	if (!isPlaying)
		startAnimation();
}
function startAnimation()
{
	isPlaying = true;
	var totalFrames = $(".animation").length;
	var currentFrame = 0;
	window.setInterval(function(){
		$(".animation").eq(currentFrame).hide();
		currentFrame = (currentFrame + 1) % totalFrames;
		$(".animation").eq(currentFrame).show();
	},500);
}

$(document).ready(function()
{
	$(".animation").first().show();
	$("#experimental").on("click", goFullScreenAndStartPlaying);
	
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