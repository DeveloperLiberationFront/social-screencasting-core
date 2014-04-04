
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
	console.log("reporting "+thing);
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

function detectedClickCommand(name) {
    queuedToolUsages.push(new ToolUsage(name, "[GUI]", ""));
}

function registerGUIClick(selector, toolName) {
	$(document).on('click', selector, function() {
			detectedClickCommand(toolName);
	});
}

function selectPlugin() {
	 queuedToolUsages.push(new ToolUsage("Changed plugin", "[GUI]", ""));
	 reportToolUsages();
}


$(document).ready(function () {
	console.log("beginning of instrumentation");
	
	$("#pluginSelector").change(selectPlugin);
	
	
	detectedClickCommand("Loaded Interface");
	setInterval(reportToolUsages, 60000);
});