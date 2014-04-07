package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.util.ToolCountStruct;

public interface WebQueryInterface {

	@Deprecated
	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	List<File> getBestExamplesOfTool(String pluginName, String toolName);

	void shareClipWithUser(String clipId, String recipient);

	void requestClipsFromUser(String owner, String pluginName, String toolName);

	List<ToolCountStruct> getAllToolAggregateForPlugin(String pluginName);

}
