
function setUpFancyBox() {
    $('.fancybox-thumbs').fancybox({
        prevEffect: 'none',
        nextEffect: 'none',

        closeBtn: false,
        arrows: false,
        nextClick: true,

        helpers: {
            thumbs: {
                width: 50,
                height: 50
            }
        }
    });
}

//To be called to dynamically allow jquery nodes to be patched into
/*function addRequestGenerationListeners() {
    $(".requestGeneration").on('click', requestGenerationOfMedia);

    setUpFancyBox();
}*/

function addResponseHTMLToWindow(data) {
    $(".modal").hide();
    $(".moreInfo").html($(data).hide());

    $(".moreInfo").children().fadeIn("fast");
    //addRequestGenerationListeners();
    setUpFancyBox();

}

function requestGenerationOfMedia() {
    $(".moreInfo").removeClass("hidden");
    $(".modal").show();
    $.post("makeVideo", {
        thingToDo: "makeVideo",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //this will return the html to view the media
}

function doesVideoExistHuh() {
    $(".moreInfo").removeClass("hidden");
    $.post("makeVideo", {
        thingToDo: "isVideoAlreadyMade",
        pluginName: $(this).data("pluginName"),
        toolName: $(this).data("toolName")
    }, addResponseHTMLToWindow);
    //This will either return the html to view the media, 
    //or some html prompting the user to make the media
}

$(document).ready(function () {
    //handles the click on the view buttons to see if a video file exists
    $(".clickMe").on('click', doesVideoExistHuh);

    $(".clickMe").on('mouseenter', function () {
        $(this).addClass("hoverOver");
    });
    $(".clickMe").on('mouseleave', function () {
        $(this).removeClass("hoverOver");
    });

    $(".moreInfo").on('click', '.requestGeneration', requestGenerationOfMedia);
});