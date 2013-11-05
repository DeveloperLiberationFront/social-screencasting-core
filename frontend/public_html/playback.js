var isPlaying = false;
var currentFrame = 0;
var animationTimer;

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
	var totalFrames = +$("#panel").data("totalFrames");
	currentFrame = 0;
	if (!isPlaying)
	{
		startAnimation();
	}
}

function startAnimation()
{
	$(".playbackCommand").removeClass("pause").addClass("play");
	isPlaying = true;
	animationTimer = window.setInterval(function(){
		$(".animation").eq(currentFrame).hide();
		currentFrame = (currentFrame + 1) % totalFrames;
		updateSlider(currentFrame);
		$(".animation").eq(currentFrame).show();
	},200);
}

function stopAnimation()
{
	if(isPlaying)
	{
		$(".playbackCommand").removeClass("play").addClass("pause");
		isPlaying = false;
		clearInterval(animationTimer);
	}
}

function playOrPause()
{
	if(isPlaying)
	{
		stopAnimation();
	}
	else
	{
		startAnimation();
	}
}

function updateSlider(index)
{
	$(".slider").slider("value",index);
}

$(document).ready(function()
{
	$(".animation").first().show();	//so the user sees something
	$("#overlay").on("click", goFullScreenAndStartPlaying);
	$("#playPause").on("click",playOrPause);
	
	totalFrames = +$("#panel").data("totalFrames");
	$(".slider").slider({
		value:0,
		min: 0,
		max: totalFrames,
		step: 1,
		animate:"fast",
		easing:"linear",
		slide: sliderMoved
	});
	preloadImages();
	
	
});

function sliderMoved( event, ui ) {
	stopAnimation();
	$(".animation").eq(currentFrame).hide();
	currentFrame = ui.value % totalFrames;
	$(".animation").eq(currentFrame).show();
	updateSlider(currentFrame);
}

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