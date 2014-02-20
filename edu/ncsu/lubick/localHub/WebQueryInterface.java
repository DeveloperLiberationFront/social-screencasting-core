package edu.ncsu.lubick.localHub;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	//List<ToolUsage> extractMediaForLastNUsagesOfTool(int n, String pluginName, String toolName) throws MediaEncodingException;

	List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName);

}
