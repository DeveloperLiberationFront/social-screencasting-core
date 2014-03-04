package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	@Deprecated
	List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName);

	List<File> getBestExamplesOfTool(String pluginName, String toolName);

}
