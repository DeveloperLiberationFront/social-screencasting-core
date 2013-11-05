

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
	launchFullScreen(document.getElementById("panel"));
}


$(document).ready(function()
{
	$("#experimental").on("click", goFullScreenAndStartPlaying);
});