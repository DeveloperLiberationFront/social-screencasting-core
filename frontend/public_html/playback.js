var isPlaying = false;
var currentFrame = 0;
var animationTimer;
var isFullScreen = false;
var totalFrames;
var animationEnabled = false;

function launchFullScreen(element) {  //From davidwalsh.name/fullscreen
    if (element.requestFullScreen) {
        element.requestFullScreen();
    } else if (element.mozRequestFullScreen) {
        element.mozRequestFullScreen();
    } else if (element.webkitRequestFullScreen) {
        element.webkitRequestFullScreen();
    }
    isFullScreen = true;
}



function setFloatingPlaybackControlsVisible(shouldBeVisible) {
    if (shouldBeVisible) {
        $("#moduleControlPanel").show();
    }
    else {
        $("#moduleControlPanel").hide();
    }
}

function updateSlider(index) {
    $(".slider").slider("value", index);
}

function startAnimation() {
    if (!isPlaying) {
        $(".playbackCommand").removeClass("pause").addClass("play");
        isPlaying = true;
        var totalFrames = +$("#panel").data("totalFrames");
        animationTimer = window.setInterval(function () {
            $(".frame").eq(currentFrame).hide();
            currentFrame = (currentFrame + 1) % totalFrames;
            updateSlider(currentFrame);
            $(".frame").eq(currentFrame).show();
        }, 200);
    }
}

function stopAnimation() {
    if (isPlaying) {
        $(".playbackCommand").removeClass("play").addClass("pause");
        isPlaying = false;
        clearInterval(animationTimer);
    }
}

function playOrPause() {
    if (isPlaying) {
        stopAnimation();
    }
    else {
        startAnimation();
    }
}

function sliderMoved(event, ui) {
    stopAnimation();
    $(".frame").eq(currentFrame).hide();
    currentFrame = ui.value % totalFrames;
    $(".frame").eq(currentFrame).show();
    updateSlider(currentFrame);
}

function getImageForFrameNumber(frameNumber) {
    var retVal = '<img class="frame" src="bug8/temp';
    if (frameNumber < 1000) {
        retVal += "0";
    }
    if (frameNumber < 100) {
        retVal += "0";
    }
    if (frameNumber < 10) {
        retVal += "0";
    }
    retVal += frameNumber;
    retVal += '.png"></img>';
    return $(retVal);
}

function preloadImages() {
    var p, i;
    p = $("#panel");
    totalFrames = +$("#panel").data("totalFrames");
    for (i = 1; i < totalFrames; i++) {
        getImageForFrameNumber(i).appendTo(p);
    }

}

$(document).keyup(function (e) {
    if (e.keyCode == 27) { //escape was pressed
        console.log("Escape");
        setFloatingPlaybackControlsVisible(false);
    }
});

function setUpSliders() {
    $(".slider").slider({
        value: 0,
        min: 0,
        max: totalFrames,
        step: 1,
        animate: "fast",
        easing: "linear",
        slide: sliderMoved	//when the user moves this, call sliderMoved()
    });
}

function setUpDraggablePlayControl() {
    $("#moduleControlPanel").draggable();
}

function goFullScreenAndStartPlaying() {
    launchFullScreen($("#panel")[0]);

    setFloatingPlaybackControlsVisible(true);
    startAnimation();
}

$(document).ready(function () {
    $(".frame").first().show();	//so the user sees something
    $("#overlay").on("click", goFullScreenAndStartPlaying);

    $(".playPause").on("click", playOrPause);

    totalFrames = +$("#panel").data("totalFrames");
    if ($("#panel").data("type") == "keystroke") {
        animationEnabled = true;
    }
    setUpSliders();
    setUpDraggablePlayControl();
    preloadImages();


});