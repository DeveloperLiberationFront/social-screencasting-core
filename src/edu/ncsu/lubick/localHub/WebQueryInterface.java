package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.util.ToolCountStruct;

public interface WebQueryInterface {

	List<String> getNamesOfAllPlugins();

	List<File> getBestExamplesOfTool(String pluginName, String toolName, boolean isKeyboardHuh);

	void shareClipWithUser(String clipId, String recipient, ClipOptions clipOptions);

	void requestClipsFromUser(String owner, String pluginName, String toolName);

	List<ToolCountStruct> getAllToolAggregateForPlugin(String pluginName);
	
	ToolCountStruct getToolAggregate(String applicationName, String toolName);

	void updateClipOptions(String folder, ClipOptions clipOptions, boolean upload);

}
