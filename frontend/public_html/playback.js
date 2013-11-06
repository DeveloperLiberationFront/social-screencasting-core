var isPlaying = false;
var currentFrame = 0;
var animationTimer;
var isFullScreen = false;
var totalFrames;
var animationEnabled = false;
var rampUp = 22;	//22 frames of ramp up
var durationOfAnimation = 10;  //10 frames of showing animation

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

function handleAnimationForFrame(currentFrame) {
    if (animationEnabled) {
        if (currentFrame < rampUp || currentFrame > (rampUp + durationOfAnimation)) {
            $(".keyAnimation.full").hide();
            $(".keyAnimation.blank").show();
        }
        else {
            $(".keyAnimation.blank").hide();
            $(".keyAnimation.full").show();
        }
    }
}

function startFramePlayback() {
    if (!isPlaying) {
        $(".playbackCommand").removeClass("pause").addClass("play");
        isPlaying = true;
        var totalFrames = +$("#panel").data("totalFrames");
        animationTimer = window.setInterval(function () {
            $(".frame").eq(currentFrame).hide();
            currentFrame = (currentFrame + 1) % totalFrames;
            updateSlider(currentFrame);
            handleAnimationForFrame(currentFrame);
            $(".frame").eq(currentFrame).show();
        }, 200);
    }
}

function stopFramePlayback() {
    if (isPlaying) {
        $(".playbackCommand").removeClass("play").addClass("pause");
        isPlaying = false;
        clearInterval(animationTimer);
    }
}

function playOrPause() {
    if (isPlaying) {
        stopFramePlayback();
    }
    else {
        startFramePlayback();
    }
}

function sliderMoved(event, ui) {
    stopFramePlayback();
    $(".frame").eq(currentFrame).hide();
    currentFrame = ui.value % totalFrames;
    handleAnimationForFrame(currentFrame);
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

function setUpDraggableThings() {
    $("#moduleControlPanel").draggable();
	//$(".animationHolder").draggable();  Not as simple as I hoped.  Abandoning for now
}

function goFullScreen() {
    launchFullScreen($("#panel")[0]);

    setFloatingPlaybackControlsVisible(true);

}

$(document).ready(function () {
    $(".frame").first().show();	//so the user sees something
    $("#overlay").on("click", goFullScreen);

    $(".playPause").on("click", playOrPause);

    totalFrames = +$("#panel").data("totalFrames");
    if ($("#panel").data("type") == "keystroke") {
        animationEnabled = true;
    }
    handleAnimationForFrame(0);
    setUpSliders();
    setUpDraggableThings();
    preloadImages();


});