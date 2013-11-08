var isPlaying;
var currentFrame;
var frameAnimationTimer;
var isFullScreen;
var totalFrames;
var animationEnabled;
var currentAnimationChoice;
var totalAnimationChoices;
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

function handleAnimationForCurrentFrame() {
    $(".keyAnimation").hide();
    if (totalAnimationChoices == 1 ||
        (totalAnimationChoices - 1) == currentAnimationChoice) {
        //if none is selected, or we don't have any animation options, do nothing
        return;
    }
    if (currentAnimationChoice === 0) {
        if (currentFrame < rampUp || currentFrame > (rampUp + durationOfAnimation)) {
            $(".keyAnimation.blank.image").show();
        }
        else {
            $(".keyAnimation.full.image").show();
        }
    } else if (currentAnimationChoice === 1) {
        if (currentFrame < rampUp || currentFrame > (rampUp + durationOfAnimation)) {
            $(".keyAnimation.blank.text").show();
        }
        else {
            $(".keyAnimation.full.text").show();
        }
    } else if (currentAnimationChoice === 2) {
        if (currentFrame < rampUp || currentFrame > (rampUp + durationOfAnimation)) {
            $(".keyAnimation.blank.imageText").show();
        }
        else {
            $(".keyAnimation.full.imageText").show();
        }
    }
}

function startFramePlayback() {
    if (!isPlaying) {
        $(".playbackCommand").removeClass("pause").addClass("play");
        isPlaying = true;

        frameAnimationTimer = window.setInterval(function () {
            $(".frame").eq(currentFrame).hide();
            currentFrame = (currentFrame + 1) % totalFrames;
            updateSlider(currentFrame);
            handleAnimationForCurrentFrame();
            $(".frame").eq(currentFrame).show();
        }, 200);
    }
}

function stopFramePlayback() {
    if (isPlaying) {
        $(".playbackCommand").removeClass("play").addClass("pause");
        isPlaying = false;
        clearInterval(frameAnimationTimer);
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
    event.preventDefault();
    stopFramePlayback();
    $(".frame").eq(currentFrame).hide();
    currentFrame = ui.value % totalFrames;
    handleAnimationForCurrentFrame();
    $(".frame").eq(currentFrame).show();
    updateSlider(currentFrame);
}

function getImageForFrameNumber(frameNumber) {
    var retVal = '<img class="frame" src="';
    retVal += $("#panel").data("toolName");
    retVal += '/frame';
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
        max: totalFrames - 1,	//minus 1 because we start at 0
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

function setAnimationOverlaysTo(animationSelection) {
    var fullScreenAnimationChoices, previewAnimationChoices;
    previewAnimationChoices = $("#controlPanel").find(".animationSelection");
    fullScreenAnimationChoices = $("#moduleControlPanel").find(".animationSelection");

    fullScreenAnimationChoices.eq(animationSelection).show();
    previewAnimationChoices.eq(animationSelection).show();

}

function rotateAnimationSettings() {

    $(".animationSelection").hide();	//hide all
    currentAnimationChoice = (currentAnimationChoice + 1) % totalAnimationChoices;

    setAnimationOverlaysTo(currentAnimationChoice);
    handleAnimationForCurrentFrame();
}


function renderPlayback() {
    isPlaying = false;
    currentFrame = 0;
    isFullScreen = false;
    animationEnabled = false;
    currentAnimationChoice = 0;
    totalAnimationChoices = $("#controlPanel").find(".animationSelection").length;
    //console.log(totalAnimationChoices + " total animation choices");
    setAnimationOverlaysTo(currentAnimationChoice);

    $(".frame").first().show();	//so the user sees something
    $("#overlay").on("click", goFullScreen);

    $(".playPause").on("click", playOrPause);
    $(".settings").on("click", rotateAnimationSettings);

    totalFrames = +$("#panel").data("totalFrames");
    if ($("#panel").data("type") == "keystroke") {
        animationEnabled = true;
    }

    $(".startLabel").css('left', rampUp*100 / totalFrames + '%');
    handleAnimationForCurrentFrame();
    setUpSliders();
    setUpDraggableThings();
    preloadImages();


}

$(document).ready(renderPlayback);