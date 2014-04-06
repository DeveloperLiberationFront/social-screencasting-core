/*global currentPlugin, ascending*/   //requires comparison.js (or whatever) to know what the current plugin is,
//if the tools are sorted ascending
var queuedToolUsages = [];

function ToolUsage(toolName, keypress, otherInfo) {
    //all the this.names are the same as on the server ToolStream.java
    this.Tool_Name = toolName;
    this.Tool_Key_Presses = keypress;
    this.Tool_Class = otherInfo;
    this.Tool_Timestamp = new Date().getTime();
    this.Tool_Duration = 1000;		//duration doesn't matter
}

function reportToolUsages() {
    var thing = JSON.stringify(queuedToolUsages), oldUsages = [];
    console.log("reporting " + thing);
    oldUsages.concat(queuedToolUsages);	//copy old ones
    queuedToolUsages = [];

    $.ajax("http://localhost:4443/reportTool", {
        type: "POST",
        data: { pluginName: "[ScreencastingHub]", toolUsages: thing },
        success: function (returnedData) {

        },
        error: function (e, reason, error) {
            console.error("There was a problem sending things to the server.  Most likely, the server was not up");
            //stick the previously queued elements onto the end of the current
            queuedToolUsages.concat(oldUsages);
        }
    });

}

function queueToolUse(tu) {
    queuedToolUsages.push(tu);
}

function detectedClickCommand(name) {
    queuedToolUsages.push(new ToolUsage(name, "[GUI]", ""));
}

function registerGUIClick(selector, toolName) {
    $(document).on('click', selector, function () {
        detectedClickCommand(toolName);
    });
}

function selectPlugin() {
    queuedToolUsages.push(new ToolUsage("Changed plugin", "[GUI]", this.value));
    reportToolUsages();
}

function monitorSorts() {
    $(document).on('click', '.sortByTool', function () {
        queueToolUse(new ToolUsage("Sort tools alphabetically", "[GUI]", "ascending: " + !ascending));
        //report !ascending because by the time this callback happens, the sort has already happened and ascending
        //refers to what the next sort should be
    });

    $(document).on('click', '.sortByNum', function () {
        queueToolUse(new ToolUsage("Sort tools by count", "[GUI]", "ascending: " + !ascending));
        //report !ascending because by the time this callback happens, the sort has already happened and ascending
        //refers to what the next sort should be
    });

    $(document).on('click', '.sortByName', function () {
        queueToolUse(new ToolUsage("Sort emails by name", "[GUI]", "ascending: " + !ascending));
        //report !ascending because by the time this callback happens, the sort has already happened and ascending
        //refers to what the next sort should be
    });

    $(document).on('click', '.sortByEmail', function () {
        queueToolUse(new ToolUsage("Sort emails by email address", "[GUI]", "ascending: " + !ascending));
        //report !ascending because by the time this callback happens, the sort has already happened and ascending
        //refers to what the next sort should be
    });
}


$(document).ready(function () {
    console.log("beginning of instrumentation");

    $("#pluginSelector").change(selectPlugin);
    $(document).on('click', '.myItem', function () {
        queueToolUse(new ToolUsage("Select Local Tool", "[GUI]", currentPlugin + ":" + $(this).data("toolName")));
    });

    $(document).on('click', '.addedUser', function () {
        queueToolUse(new ToolUsage("View tools of user", "[GUI]",
		$(this).children().eq(1).text() +	// selects the second <td> which is the email address
		":" + currentPlugin));
    });

    monitorSorts();

    detectedClickCommand("Loaded Interface");
    setInterval(reportToolUsages, 60000);
});