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

function preloadImages()
{
	var p = $("#panel");
	$('<img class="animation" src="bug8/temp0001.png"></img>').appendTo(p);
	$('<img class="animation" src="bug8/temp0002.png"></img>').appendTo(p);
	$('<img class="animation" src="bug8/temp0003.png"></img>').appendTo(p);
	$('<img class="animation" src="bug8/temp0004.png"></img>').appendTo(p);
}