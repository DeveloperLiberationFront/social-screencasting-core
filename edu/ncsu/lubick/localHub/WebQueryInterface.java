package edu.ncsu.lubick.localHub;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	@Deprecated
	ToolUsage extractMediaForLastUsageOfTool(String pluginName, String toolName) throws MediaEncodingException;

	@Deprecated
	ToolUsage getLastInstanceOfToolUsage(String pluginName, String toolName);

	List<ToolUsage> extractMediaForLastNUsagesOfTool(int n, String pluginName, String toolName) throws MediaEncodingException;

	List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName);

}
