var isPlaying;
var currentFrame;
var frameAnimationTimer;
var isFullScreen;
var totalFrames;
var startFrames = 0;
var endFrames;
var animationEnabled;
var currentAnimationChoice;
var totalAnimationChoices;
var rampUp = 22;	//22 frames of ramp up
var durationOfAnimation = 10;  //10 frames of showing animation
var authToken = "";
var hasInitializedButtons = false;

function launchFullScreen(element) {  //From davidwalsh.name/fullscreen
    if (element.requestFullScreen) {
        element.requestFullScreen();
    } else if (element.mozRequestFullScreen) {
        element.mozRequestFullScreen();
    } else if (element.webkitRequestFullScreen) {
        element.webkitRequestFullScreen();
    }
}

$(window).bind("fullscreen-toggle", function(e, state) {
    isFullScreen = state;
    setFloatingPlaybackControlsVisible(state);
});

function setFloatingPlaybackControlsVisible(shouldBeVisible) {
    if (shouldBeVisible) {
        $("#moduleControlPanel").show();
    }
    else {
        $("#moduleControlPanel").hide();
    }
}

function updateSlider(index) {
    $("#staticSlider, #dockedSlider").slider("value", index);
}

function handleAnimationOptionsForCurrentFrame() {
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
            var oldFrame = $(".frame").eq(currentFrame);
            currentFrame = currentFrame + 1;

			if (currentFrame >= endFrames) {
                $(".playPause").prop("disabled", true);
                stopFramePlayback(false);

                oldFrame.animate({opacity: 0}, 500, function() {
                    setTimeout(function() {
                        currentFrame = startFrames;
                        oldFrame.hide();
                        oldFrame.css({opacity: 100});
                        $(".frame").eq(currentFrame).show();
                        startFramePlayback();
                        $(".playPause").prop("disabled", false);
                    }, 500);
				});
			} else if (currentFrame <= startFrames + 1) {
                $(".playPause").prop("disabled", true);
                currentFrame = startFrames;
                $(".frame").eq(currentFrame).css({opacity: 0});
                stopFramePlayback(false);
                $(".frame").eq(currentFrame).animate({opacity: 1.0}, 500, function() {
                    startFramePlayback();
                    $(".frame").eq(currentFrame).hide();
                    $(".frame").eq(currentFrame).css({opacity: 0});
                    currentFrame++;
                    $(".frame").eq(currentFrame).show();
                    $(".playPause").prop("disabled", false);
                });

            } else {
                oldFrame.hide();
                updateSlider(currentFrame);
                handleAnimationOptionsForCurrentFrame();
                $(".frame").eq(currentFrame).show();
            }

        }, 200);
    }
}

function stopFramePlayback(updatePausePlayButton) {
    if (isPlaying) {
        if(updatePausePlayButton) {
            $(".playbackCommand").removeClass("play").addClass("pause");
        }
        isPlaying = false;
        clearInterval(frameAnimationTimer);
    }
}

function playOrPause() {
    if (isPlaying) {
        stopFramePlayback(true);
    }
    else {
        startFramePlayback(true);
    }
}

function sliderMoved(event, ui) {
    event.preventDefault();

    if(ui.value != currentFrame) {
        stopFramePlayback(true);
        $(".frame").eq(currentFrame).hide();
        currentFrame = ui.value % totalFrames;
        handleAnimationOptionsForCurrentFrame();
        $(".frame").eq(currentFrame).show();
    
        updateSlider(currentFrame);
    }
}

function getImageForFrameNumber(frameNumber) {
    var retVal = '<img class="frame" src="';
    retVal += $("#panel").data("playbackDirectory");
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
    retVal += '.jpg' + authToken + '"></img>';
    return $(retVal);
}

function preloadImages() {
    var p, i;
    p = $("#panel");
    for (i = 0; i < totalFrames; i++) {
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
    $("#staticSlider, #dockedSlider").slider({
        value: startFrames,
        min: 0,
        max: totalFrames - 1,	//minus 1 because we start at 0
        animate: "fast",
        step: 1,
        easing: "linear",
        slide: sliderMoved,
        change: sliderMoved	//when the user moves this, call sliderMoved()
    });
	
	$("#editSlider").slider({
		range: true,
        values: [startFrames, endFrames],
        min: 0,
        animate: "fast",
        max: totalFrames - 1,	//minus 1 because we start at 0
        step: 1,
        easing: "linear",
        slide: setMinMaxFrame	//when the user moves this, call sliderMoved()
    });
	
}

function setMinMaxFrame(event, ui) {
   startFrames = ui.values[0];
   endFrames = ui.values[1];
   stopFramePlayback(true);

   $(".frame").css({opacity: 1.0});
   $(".frame").eq(startFrames).css({opacity: 0.0});
   
   if (startFrames > currentFrame) {
    $("#staticSlider, #dockedSlider").slider("value", startFrames);
   }

   if (endFrames < currentFrame) {
    $("#staticSlider, #dockedSlider").slider("value", endFrames);
   }
}

function saveVideoLength () {
	//currentlydoesnothing
	/* var postURL, emailToRequest;

    postURL = "/shareRequest";

    $.ajax({
        type: "POST",
        url: postURL,
        data: { "pluginName": currentPlugin, "toolName": currentTool, "ownerEmail": currentEmail }

    });

    requested[currentEmail + currentPlugin + currentTool] = true;*/

}

function setUpDraggableThings() {
    $("#moduleControlPanel").draggable();
    //$(".animationHolder").draggable();  Not as simple as I hoped.  Abandoning for now
}

function setAnimationOverlaysTo(animationSelection) {
    var fullScreenAnimationChoices, previewAnimationChoices;
    previewAnimationChoices = $("#controlPanel").find(".animationSelection");
    fullScreenAnimationChoices = $("#moduleControlPanel").find(".animationSelection");

    if (totalAnimationChoices > 1)		//leave out "no animation" interference
    {
        localStorage.setItem("defaultAnimationSetting", animationSelection);
    }

    fullScreenAnimationChoices.eq(animationSelection).show();
    previewAnimationChoices.eq(animationSelection).show();

}

function rotateAnimationSettings() {

    $(".animationSelection").hide();	//hide all
    currentAnimationChoice = (currentAnimationChoice + 1) % totalAnimationChoices;

    setAnimationOverlaysTo(currentAnimationChoice);
    handleAnimationOptionsForCurrentFrame();
}

function renderPlayback(auth) {
    if (auth) {
        authToken = auth;
    }
	else
	{
		authToken = "";
	}
    isPlaying = false;
    currentFrame = startFrames;
    isFullScreen = false;
    animationEnabled = false;
    currentAnimationChoice = 0;
    totalAnimationChoices = $("#controlPanel").find(".animationSelection").length;

    var defaultAnimationChoice = +localStorage.getItem("defaultAnimationSetting");
    if (defaultAnimationChoice !== null &&
        defaultAnimationChoice <= (totalAnimationChoices - 1)) {
        currentAnimationChoice = defaultAnimationChoice;
    }

    setAnimationOverlaysTo(currentAnimationChoice);

	if (!hasInitializedButtons)
	{
		$("#overlay").on("click", function() {
            launchFullScreen($("#panel")[0])

        });
		
        $("#moreInfo").on("click",".playPause", playOrPause);
		$("#moreInfo").on("click",".settings", rotateAnimationSettings);
		$("moreInfo").on("click",".save", saveVideoLength);
		hasInitializedButtons = true;
	}
    
    totalFrames = $("#panel").data("totalFrames");
    if(endFrames <= 0) {
        endFrames = totalFrames - 1;
    }

    if ($("#panel").data("type") == "keystroke") {
        animationEnabled = true;
    }

	//put a little green tick where the tool starts
    $(".startLabel").css('left', rampUp * 100 / endFrames + '%');
    handleAnimationOptionsForCurrentFrame();
    setUpSliders();
    setUpDraggableThings();
    preloadImages();
    $(".frame").eq(startFrames).css({opacity: 0});
	$(".frame").eq(startFrames).show();

}
