package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	List<File> getBestExamplesOfTool(String pluginName, String toolName);

	void shareClipWithUser(String clipId, String recipient);

}
