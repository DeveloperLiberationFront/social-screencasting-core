/*global renderPlayback */  // needs playback.js

var authToken, refToImages;

function setUpAnimations(imageAssets) {
    //add in animation frames
    var keyInsertionPoint, firstOptionIconInsertionPoint, secondOptionIconInsertionPoint;
    keyInsertionPoint = $("#keyAnimationSpot");
    firstOptionIconInsertionPoint = $("#animationSelectionSpot1");
    secondOptionIconInsertionPoint = $("#animationSelectionSpot2");

    $(".animationSelection").remove();
    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/none.png" />');
    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/none.png" />');

    if (imageAssets.length === 0) {
        return;
    }

    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageAndText.png" />');
    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/textOnly.png" />');
    firstOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageOnly.png" />');

    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageAndText.png" />');
    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/textOnly.png" />');
    secondOptionIconInsertionPoint.after('<img class="animationSelection" src="/images/imageOnly.png" />');

    keyInsertionPoint.after('<img class="keyAnimation full imageText" src="' + refToImages + '/image_text.png' + authToken + '" />');
    keyInsertionPoint.after('<img class="keyAnimation blank imageText" src="' + refToImages + '/image_text_un.png' + authToken + '" />');

    keyInsertionPoint.after('<img class="keyAnimation full text" src="' + refToImages + '/text.png' + authToken + '" />');
    keyInsertionPoint.after('<img class="keyAnimation blank text" src="' + refToImages + '/text_un.png' + authToken + '" />');

    keyInsertionPoint.after('<img class="keyAnimation full image" src="' + refToImages + '/image.png' + authToken + '" />');
    keyInsertionPoint.after('<img class="keyAnimation blank image" src="' + refToImages + '/image_un.png' + authToken + '" />');
}

function setUpPlaybackForDataAuthAndDir(data, auth, imageDir) {
    var pluginName, toolName, frames, imageAssets, i;
    authToken = auth;
    refToImages = imageDir;
	
	/*
		Will set up Playback given an auth token (can be blank for local service), an image dir (can be screencaster-hub.appspot.com/whatever for
		external) and a data package that looks like
		{clip: { filenames : ["frame0000.jpg", "frame0001.jpg"...], name: "CLIP ID", plugin: "Eclipse", tool: "Organize Imports"}}
	*/

    //console.log(data);

    pluginName = data.clip.plugin;
    toolName = data.clip.tool;
    frames = data.clip.filenames;
    for (i = frames.length - 1; i > 0; i--) {
        if (frames[i].lastIndexOf("frame", 0) === 0)	//http://stackoverflow.com/a/4579228/1447621
        {
            break;
        }
    }

    imageAssets = frames.splice(i + 1, frames.length - i - 1);	//animations


    $("#mediaTitle").text(toolName);
    //set up required data for playback.js to render
    $("#panel").data("totalFrames", frames.length)
				.data("type", (imageAssets.length === 0 ? "menu" : "keystroke"))
				.data("playbackDirectory", refToImages)
				.data("auth");

    setUpAnimations(imageAssets);

    renderPlayback(authToken);
    $("#clipLoading").hide();
    $("#clipPlayer").show();
}